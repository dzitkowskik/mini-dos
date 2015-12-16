package pl.pw.edu.mini.dos.node.ndb;

import net.sf.jsqlparser.statement.SetStatement;
import net.sf.jsqlparser.statement.StatementVisitor;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Helper;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.masternode.ExecuteSQLOnNodeResponse;
import pl.pw.edu.mini.dos.communication.nodemaster.*;
import pl.pw.edu.mini.dos.communication.nodenode.*;
import pl.pw.edu.mini.dos.node.task.TaskManager;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class SQLStatementVisitor implements StatementVisitor {
    private static final Logger logger = LoggerFactory.getLogger(SQLStatementVisitor.class);
    private NodeMasterInterface master;
    private NodeNodeInterface thisNode;
    private ExecuteSQLOnNodeResponse result;
    private Long taskId;

    public SQLStatementVisitor(
            NodeMasterInterface master, NodeNodeInterface node, Long taskId) {
        this.master = master;
        this.taskId = taskId;
        this.thisNode = node;
    }

    public ExecuteSQLOnNodeResponse getResult() {
        return this.result;
    }

    @Override
    public void visit(Select select) {
        logger.info("Coordinator node: select request");
        String selectStatement = select.toString();
        logger.debug(selectStatement);

        // Get tables names
        SQLSelectTablesFinder tablesFinder = new SQLSelectTablesFinder();
        List<String> tablesNames = tablesFinder.getTableList(select);
        logger.debug("Tables: " + Helper.collectionToString(tablesNames));

        // Get nodes to select data from and create tables
        SelectMetadataResponse selectMetadataResponse;
        try {
            selectMetadataResponse =
                    master.selectMetadata(new SelectMetadataRequest(tablesNames));
        } catch (RemoteException e) {
            logger.error("Cannot get select metadata from master: {}", e.getMessage());
            this.result = new ExecuteSQLOnNodeResponse("", ErrorEnum.REMOTE_EXCEPTION);
            return;
        }

        if (!selectMetadataResponse.getError().equals(ErrorEnum.NO_ERROR)) {
            logger.error("Error at inserting metadata in master");
            this.result = new ExecuteSQLOnNodeResponse("", selectMetadataResponse.getError());
            return;
        }

        // Create in-memory db
        InDBmanager inDbManager = new InDBmanager();
        // Create needed tables
        inDbManager.createTables(selectMetadataResponse.getCreateTableStatements());

        // For each table
        ErrorEnum error = ErrorEnum.NO_ERROR;
        Map<String, List<String>> versionsOfTables = new HashMap<>(tablesNames.size());
        for (String table : tablesNames) {
            // Send requests to get the needed data
            String selectAll = "SELECT * FROM " + table + ";";
            for (NodeNodeInterface node : selectMetadataResponse.getNodes()) {
                try {
                    node.executeSql(new ExecuteSqlRequest(
                            taskId, selectAll, thisNode));
                } catch (RemoteException e) {
                    logger.error("Cannot get data from another node: {}", e.getMessage());
                    error = ErrorEnum.REMOTE_EXCEPTION;
                }
            }
            // Get responses and proccess data
            List<String> versionsOfTable = new ArrayList<>(selectMetadataResponse.getNodes().size());
            for (int i = 0; i < selectMetadataResponse.getNodes().size(); i++) {
                NodeNodeInterface node = selectMetadataResponse.getNodes().get(i);
                GetSqlResultResponse response = null;
                try {
                    response = node.getSqlResult(new GetSqlResultRequest(taskId));
                } catch (RemoteException e) {
                    logger.error("Cannot get data from another node: {}", e.getMessage());
                    error = ErrorEnum.REMOTE_EXCEPTION;
                } catch (ExecutionException e) {
                    logger.error("Error at getting result: {}", e.getMessage());
                    error = ErrorEnum.REMOTE_EXCEPTION;
                } catch (InterruptedException e) {
                    logger.error("Operation interrupted", e.getMessage());
                    error = ErrorEnum.ANOTHER_ERROR;
                }
                if (response == null || !response.getError().equals(ErrorEnum.NO_ERROR)) {
                    // Last error is stored
                    error = response == null ? ErrorEnum.ANOTHER_ERROR : response.getError();
                } else {
                    // Import received table to in-memory db
                    String versionOftable = table + i;
                    versionsOfTable.add(versionOftable);
                    boolean ok = inDbManager.importTable(versionOftable,
                            response.getData().getColumnsTypes(),
                            response.getData().getColumnsNames(),
                            response.getData().getData());
                    if (!ok) {
                        logger.error("Error at importing table to imdb");
                        error = ErrorEnum.ANOTHER_ERROR;
                    }
                }
            }
            versionsOfTables.put(table, versionsOfTable);
        }

        // Create temportal table merging all versions of the table received
        for (String table : tablesNames) {
            boolean ok = inDbManager.mergeVersionsOfTable(table, versionsOfTables.get(table));
            if (!ok) {
                logger.error("Error at merging versions of table");
                error = ErrorEnum.ANOTHER_ERROR;
            }
        }

        // Run select in temporal table
        String selectStatementTmp = selectStatement;
        for (String table : tablesNames) {
            selectStatementTmp = selectStatementTmp.replaceAll(table, table + "_tmp");
        }
        logger.debug("Execute select from tmp tables:\n" + selectStatementTmp);
        String result = inDbManager.executeSelect(selectStatementTmp);

        // TODO drop temporal tables
        // Close in-memory db
        inDbManager.close();

        // Check final result
        if (!error.equals(ErrorEnum.NO_ERROR)) {
            logger.error("Error at proccessing the select");
            this.result = new ExecuteSQLOnNodeResponse(
                    "Error at proccessing the select", error);
            return;
        }

        logger.info("Coordinator node: select proccessed!");
        this.result = new ExecuteSQLOnNodeResponse(result, error);
    }


    @Override
    public void visit(Delete delete) {

    }

    @Override
    public void visit(Update update) {

    }

    @Override
    public void visit(Insert insert) {
        logger.info("Coordinator node: insert request");
        // Get table name
        String tableName = insert.getTable().getName();
        logger.debug("Insert data in table " + tableName);

        // Get nodes to insert data into
        InsertMetadataResponse insertMetadataResponse;
        try {
            insertMetadataResponse =
                    master.insertMetadata(new InsertMetadataRequest(tableName));
        } catch (RemoteException e) {
            logger.error("Cannot get insert metadata from master: {}", e.getMessage());
            this.result = new ExecuteSQLOnNodeResponse("", ErrorEnum.REMOTE_EXCEPTION);
            return;
        }

        if (!insertMetadataResponse.getError().equals(ErrorEnum.NO_ERROR)) {
            logger.error("Error at inserting metadata in master");
            this.result = new ExecuteSQLOnNodeResponse("", insertMetadataResponse.getError());
            return;
        }

        // Add rowID and version columss
        String insertStatement = insert.toString();
        insertStatement = insertStatement.substring(0, insertStatement.lastIndexOf(')'));
        insertStatement += "," + insertMetadataResponse.getRowId();
        insertStatement += "," + taskId + ");";

        // Register task and subtasks
        int numSubTasks = insertMetadataResponse.getNodes().size();
        TaskManager.getInstance().add(taskId, numSubTasks);

        // Insert data to nodes pointed by master
        // Send requests
        for (NodeNodeInterface node : insertMetadataResponse.getNodes()) {
            try {
                node.executeSql(new ExecuteSqlRequest(
                        taskId, insertStatement, thisNode));
            } catch (RemoteException e) {
                logger.error("Cannot insert table in another node: {}", e.getMessage());
                TaskManager.getInstance().updateSubTask(
                        taskId, ErrorEnum.REMOTE_EXCEPTION);
            }
        }
        // Wait for completiton of subtasks
        TaskManager.getInstance().waitForCompletion(taskId);
        // Get responses
        ErrorEnum error = ErrorEnum.NO_ERROR;
        for (NodeNodeInterface node : insertMetadataResponse.getNodes()) {
            GetSqlResultResponse response = null;
            try {
                response = node.getSqlResult(new GetSqlResultRequest(taskId));
            } catch (RemoteException e) {
                logger.error("Cannot insert table in another node: {}", e.getMessage());
                error = ErrorEnum.REMOTE_EXCEPTION;
            } catch (ExecutionException e) {
                logger.error("Error at getting result: {}", e.getMessage());
                error = ErrorEnum.REMOTE_EXCEPTION;
            } catch (InterruptedException e) {
                logger.error("Operation interrupted", e.getMessage());
                error = ErrorEnum.ANOTHER_ERROR;
            }
            if (response != null && !response.getError().equals(ErrorEnum.NO_ERROR)) {
                error = response.getError(); // Last error is stored
            }
        }

        // Check final result
        if (!error.equals(ErrorEnum.NO_ERROR)) {
            logger.error("Error at inserting data into " + tableName);
            this.result = new ExecuteSQLOnNodeResponse(
                    "Error at inserting data into " + tableName, error);
            return;
        }
        logger.info("Coordinator node: data inserted into {}!", tableName);
        this.result = new ExecuteSQLOnNodeResponse(
                "Success in inserting data into " + tableName, error);
        // Remove task
        TaskManager.getInstance().removeTask(taskId);
    }

    @Override
    public void visit(Replace replace) {

    }

    @Override
    public void visit(Drop drop) {

    }

    @Override
    public void visit(Truncate truncate) {

    }

    @Override
    public void visit(CreateIndex createIndex) {

    }

    @Override
    public void visit(CreateTable createTable) {
        logger.info("Coordinator node: createTable request");
        // Get table name
        String tableName = createTable.getTable().getName();
        // Get create statement and add rowID and version columss
        String createStatement = createTable.toString();
        createStatement = createStatement.substring(0, createStatement.lastIndexOf(')'));
        createStatement += ", row_id INTEGER PRIMARY KEY NOT NULL";
        createStatement += ", version INTEGER NOT NULL";
        createStatement += ")WITHOUT ROWID;";
        logger.debug(createStatement + " --> " + tableName);

        // Send metadata to master
        CreateMetadataResponse createMetadataResponse;
        try {
            createMetadataResponse =
                    master.createMetadata(new CreateMetadataRequest(
                            tableName, createStatement));
        } catch (RemoteException e) {
            logger.error("Cannot insert metadata in master: {}", e.getMessage());
            this.result = new ExecuteSQLOnNodeResponse("", ErrorEnum.REMOTE_EXCEPTION);
            return;
        }

        if (!createMetadataResponse.getError().equals(ErrorEnum.NO_ERROR)) {
            logger.error("Error at inserting metadata in master");
            this.result = new ExecuteSQLOnNodeResponse("", createMetadataResponse.getError());
            return;
        }
        logger.trace("Got metadata from master");

        // Register task and subtasks
        int numSubTasks = createMetadataResponse.getNodes().size();
        TaskManager.getInstance().add(taskId, numSubTasks);
        logger.trace("Task registered");

        // Create table in nodes pointed by master
        // Send requests
        for (NodeNodeInterface node : createMetadataResponse.getNodes()) {
            try {
                node.executeSql(new ExecuteSqlRequest(
                        taskId, createStatement, thisNode));
            } catch (RemoteException e) {
                logger.error("Cannot create table in another node: {}", e.getMessage());
                TaskManager.getInstance().updateSubTask(
                        taskId, ErrorEnum.REMOTE_EXCEPTION);
            }
        }
        logger.trace("Requests sended");

        // Wait for completiton of subtasks
        TaskManager.getInstance().waitForCompletion(taskId);
        logger.trace("Finished waiting for completition");

        // Get responses
        ErrorEnum error = ErrorEnum.NO_ERROR;
        for (NodeNodeInterface node : createMetadataResponse.getNodes()) {
            GetSqlResultResponse response = null;
            try {
                response = node.getSqlResult(new GetSqlResultRequest(taskId));
            } catch (RemoteException e) {
                logger.error("Cannot create table in another node: {}", e.getMessage());
                error = ErrorEnum.REMOTE_EXCEPTION;
            } catch (ExecutionException e) {
                logger.error("Error at getting result: {}", e.getMessage());
                error = ErrorEnum.REMOTE_EXCEPTION;
            } catch (InterruptedException e) {
                logger.error("Operation interrupted", e.getMessage());
                error = ErrorEnum.ANOTHER_ERROR;
            }
            if (response != null && !response.getError().equals(ErrorEnum.NO_ERROR)) {
                error = response.getError(); // Last error is stored
            }
        }
        logger.trace("Got responses");

        // Check final result
        if (!error.equals(ErrorEnum.NO_ERROR)) {
            logger.error("Error at creating table " + tableName);
            this.result = new ExecuteSQLOnNodeResponse(
                    "Error at creating table " + tableName, error);
            return;
        }
        logger.info("Coordinator node: table {} created!", tableName);
        this.result = new ExecuteSQLOnNodeResponse(
                "Success in creating table " + tableName, error);
        // Remove task
        TaskManager.getInstance().removeTask(taskId);
    }

    @Override
    public void visit(CreateView createView) {

    }

    @Override
    public void visit(Alter alter) {

    }

    @Override
    public void visit(Statements statements) {

    }

    @Override
    public void visit(Execute execute) {

    }

    @Override
    public void visit(SetStatement setStatement) {

    }
}
