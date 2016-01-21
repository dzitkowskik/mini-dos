package pl.pw.edu.mini.dos.node.ndb;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
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
import java.util.stream.Collectors;

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

    @SuppressWarnings("ConstantConditions")
    @Override
    public void visit(Select select) {
        logger.trace("Coordinator node: select request");
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
        ImDBmanager imDbManager = new ImDBmanager();

        // Create needed tables
        imDbManager.createTables(selectMetadataResponse.getCreateTableStatements());

        // For each table
        ErrorEnum error = ErrorEnum.NO_ERROR;
        Map<String, List<String>> versionsOfTables = new HashMap<>(tablesNames.size());
        Map<String, List<String>> tablesColumnsNames = new HashMap<>(tablesNames.size());
        for (String table : tablesNames) {
            // Send requests to get the needed data
            String selectAll = "SELECT * FROM " + table + ";";
            logger.debug("Sending SELECT * to " +
                    selectMetadataResponse.getTableNodes().get(table).size() + " nodes");
            for (NodeNodeInterface node : selectMetadataResponse.getTableNodes().get(table)) {
                try {
                    node.executeSql(new ExecuteSqlRequest(
                            taskId, selectAll, thisNode));
                } catch (RemoteException e) {
                    logger.error("Cannot get data from another node: " + e.getMessage());
                    error = ErrorEnum.REMOTE_EXCEPTION;
                }
            }
            // Get responses and proccess data
            List<String> versionsOfTable = new ArrayList<>(selectMetadataResponse.getTableNodes().size());
            for (int i = 0; i < selectMetadataResponse.getTableNodes().get(table).size(); i++) {
                logger.debug("Getting response from node " + i);
                NodeNodeInterface node = selectMetadataResponse.getTableNodes().get(table).get(i);
                GetSqlResultResponse response = null;
                //noinspection Duplicates
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
                    String versionOftable = table + "_v" + i;
                    versionsOfTable.add(versionOftable);
                    boolean ok = imDbManager.importTable(versionOftable, response.getData());
                    if (!ok) {
                        logger.error("Error at importing table to imdb");
                        error = ErrorEnum.ANOTHER_ERROR;
                    }
                }
                tablesColumnsNames.put(table, response.getData().getColumnsNames());
            }
            versionsOfTables.put(table, versionsOfTable);
        }

        // Create temportal table merging all versions of the table received
        for (String table : tablesNames) {
            boolean ok = imDbManager.mergeVersionsOfTable(table,
                    versionsOfTables.get(table), tablesColumnsNames.get(table));
            if (!ok) {
                logger.error("Error at merging versions of table");
                error = ErrorEnum.ANOTHER_ERROR;
            }
        }

        // Run select in temporal table
        logger.debug("Execute select from in-memory tables:\n" + selectStatement);
        String result = imDbManager.executeSelect(selectStatement);

        // Close in-memory db
        imDbManager.close();

        // Check final result
        if (!error.equals(ErrorEnum.NO_ERROR)) {
            logger.error("Error at proccessing the select");
            this.result = new ExecuteSQLOnNodeResponse(
                    "Error at proccessing the select", error);
            return;
        }

        logger.trace("Coordinator node: select proccessed!");
        this.result = new ExecuteSQLOnNodeResponse(result, error);
    }

    @Override
    public void visit(Delete delete) {
        String requestName = "delete from table";
        logger.trace("Coordinator node: delete request");
        // Get table name
        String tableName = delete.getTable().getName();
        logger.debug("Delete data from table " + tableName);

        // Get nodes that contain rows from this table
        DeleteMetadataResponse deleteMetadataResponse;
        try {
            deleteMetadataResponse =
                    master.deleteMetadata(new DeleteMetadataRequest(tableName));
        } catch (RemoteException e) {
            logger.error("Cannot get delete metadata from master: {}", e.getMessage());
            this.result = new ExecuteSQLOnNodeResponse("", ErrorEnum.REMOTE_EXCEPTION);
            return;
        }

        if (!deleteMetadataResponse.getError().equals(ErrorEnum.NO_ERROR)) {
            logger.error("Error in getting delete metadata from master");
            this.result = new ExecuteSQLOnNodeResponse("", deleteMetadataResponse.getError());
            return;
        }

        // Register task and subtasks
        int numSubTasks = deleteMetadataResponse.getNodes().size();
        TaskManager.getInstance().add(taskId, numSubTasks);

        // Delete data from nodes that have any data from specified table
        sendRequests(deleteMetadataResponse.getNodes(), delete.toString()
                + " AND " + taskId + " > version", requestName);

        // Wait for completiton of subtasks
        TaskManager.getInstance().waitForCompletion(taskId);

        // Get responses
        ErrorEnum error = getResponses(deleteMetadataResponse.getNodes(), requestName);

        // Check final result
        checkFinalResult(error, requestName, tableName);

        // Remove task
        TaskManager.getInstance().removeTask(taskId);
    }

    @Override
    public void visit(Update update) {
        String requestName = "update in table";
        logger.trace("Coordinator node: update request");

        // Get table names
        List<String> tableNames = update.getTables().stream().map(Table::getName).collect(Collectors.toList());

        logger.debug("Update data in tables " + tableNames);

        // Get nodes that contain rows from this table
        UpdateMetadataResponse updateMetadataResponse;

        // Add taskId > version to where clause
        Expression newVersion = new LongValue(taskId);
        Column versionColumn = new Column(null, "version");
        GreaterThan gt = new GreaterThan();
        gt.setLeftExpression(newVersion);
        gt.setRightExpression(versionColumn);
        update.setWhere(update.getWhere().toString().isEmpty() ? gt : new AndExpression(update.getWhere(), gt));

        // Add version = taskId to set clause
        update.getExpressions().add(newVersion);
        update.getColumns().add(versionColumn);

        logger.debug("Performing update: {}", update.toString());

        try {
            updateMetadataResponse =
                    master.updateMetadata(new UpdateMetadataRequest(tableNames));
        } catch (RemoteException e) {
            logger.error("Cannot get update metadata from master: {}", e.getMessage());
            this.result = new ExecuteSQLOnNodeResponse("", ErrorEnum.REMOTE_EXCEPTION);
            return;
        }

        if (!updateMetadataResponse.getError().equals(ErrorEnum.NO_ERROR)) {
            logger.error("Error in getting update metadata from master");
            this.result = new ExecuteSQLOnNodeResponse("", updateMetadataResponse.getError());
            return;
        }

        // Register task and subtasks
        int numSubTasks = updateMetadataResponse.getNodes().size();
        TaskManager.getInstance().add(taskId, numSubTasks);

        // Update data in nodes that have any data from specified table
        sendRequests(updateMetadataResponse.getNodes(), update.toString(), requestName);

        // Wait for completiton of subtasks
        TaskManager.getInstance().waitForCompletion(taskId);

        // Get responses
        ErrorEnum error = getResponses(updateMetadataResponse.getNodes(), requestName);

        // Check final result
        checkFinalResult(error, requestName, tableNames.toString());

        // Remove task
        TaskManager.getInstance().removeTask(taskId);
    }

    @Override
    public void visit(Insert insert) {
        String requestName = "insert to table";
        logger.trace("Coordinator node: insert request");
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
        sendRequests(insertMetadataResponse.getNodes(), insertStatement, requestName);

        // Wait for completiton of subtasks
        TaskManager.getInstance().waitForCompletion(taskId);

        // Get responses
        ErrorEnum error = getResponses(insertMetadataResponse.getNodes(), requestName);

        // Check final result
        checkFinalResult(error, requestName, tableName);

        // Remove task
        TaskManager.getInstance().removeTask(taskId);
    }

    @Override
    public void visit(Replace replace) {

    }

    @Override
    public void visit(Drop drop) {
        // Get table name
        String tableName = drop.getName().getName();

        // Get create statement and add rowID and version columss
        String statement = drop.toString();

        // Perform table statement
        performTableStatement("drop table", tableName, statement);
    }

    @Override
    public void visit(Truncate truncate) {

    }

    @Override
    public void visit(CreateIndex createIndex) {
        // Get table name
        String tableName = createIndex.getTable().getName();

        // Get create statement and add rowID and version columss
        String statement = createIndex.toString();

        // Perform table statement
        performTableStatement("create index", tableName, statement);
    }

    @Override
    public void visit(CreateTable createTable) {
        // Get table name
        String tableName = createTable.getTable().getName();

        // Get create statement and add rowID and version columss
        String createStatement = createTable.toString();
        createStatement = createStatement.substring(0, createStatement.lastIndexOf(')'));
        createStatement += ", row_id INTEGER PRIMARY KEY NOT NULL";
        createStatement += ", version INTEGER NOT NULL";
        createStatement += ")WITHOUT ROWID;";
        logger.debug(createStatement + " --> " + tableName);

        // Perform table statement
        performTableStatement("create table", tableName, createStatement);
    }

    @Override
    public void visit(CreateView createView) {

    }

    @Override
    public void visit(Alter alter) {
        // Get table name
        String tableName = alter.getTable().getName();

        // Get create statement and add rowID and version columss
        String statement = alter.toString();

        // Perform table statement
        performTableStatement("alter table", tableName, statement);
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

    private void sendRequests(
            List<NodeNodeInterface> nodes,
            String sql,
            String requestName
    ) {
        for (NodeNodeInterface node : nodes) {
            try {
                node.executeSql(new ExecuteSqlRequest(
                        taskId, sql, thisNode));
            } catch (RemoteException e) {
                logger.error("Cannot {} in another node: {}", requestName, e.getMessage());
                TaskManager.getInstance().updateSubTask(
                        taskId, ErrorEnum.REMOTE_EXCEPTION);
            }
        }
        logger.trace("Requests sended");
    }

    private ErrorEnum getResponses(
            List<NodeNodeInterface> nodes,
            String requestName
    ) {
        ErrorEnum error = ErrorEnum.NO_ERROR;
        for (NodeNodeInterface node : nodes) {
            GetSqlResultResponse response = null;
            //noinspection Duplicates
            try {
                response = node.getSqlResult(new GetSqlResultRequest(taskId));
            } catch (RemoteException e) {
                logger.error("Cannot {} in another node: {}", requestName, e.getMessage());
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
        return error;
    }

    private void checkFinalResult(
            ErrorEnum error,
            String requestName,
            String tableName
    ) {
        if (!error.equals(ErrorEnum.NO_ERROR)) {
            String errString = "Error at " + requestName + " (table: " + tableName + ")";
            logger.error(errString);
            this.result = new ExecuteSQLOnNodeResponse(errString, error);
            return;
        }
        logger.trace("Coordinator node: {} request END", requestName);
        this.result = new ExecuteSQLOnNodeResponse(requestName + " SUCCESS", error);
    }

    private void performTableStatement(
            String requestName,
            String tableName,
            String sql
    ) {
        logger.trace("Coordinator node: {} request START", requestName);

        TableMetadataResponse tableMetadataResponse;
        try {
            tableMetadataResponse =
                    master.tableMetadata(new TableMetadataRequest(tableName, sql));
        } catch (RemoteException e) {
            logger.error("Cannot get metadata from master: {}", e.getMessage());
            this.result = new ExecuteSQLOnNodeResponse("", ErrorEnum.REMOTE_EXCEPTION);
            return;
        }

        if (!tableMetadataResponse.getError().equals(ErrorEnum.NO_ERROR)) {
            logger.error("Error while getting metadata from master: {}", tableMetadataResponse.getError());
            this.result = new ExecuteSQLOnNodeResponse("", tableMetadataResponse.getError());
            return;
        }
        logger.trace("Got metadata from master");

        // Register task and subtasks
        int numSubTasks = tableMetadataResponse.getNodes().size();
        TaskManager.getInstance().add(taskId, numSubTasks);
        logger.trace("Task registered");

        // Send requests
        sendRequests(tableMetadataResponse.getNodes(), sql, requestName);

        // Wait for completiton of subtasks
        TaskManager.getInstance().waitForCompletion(taskId);
        logger.trace("Finished waiting for completition");

        // Get responses
        ErrorEnum error = getResponses(tableMetadataResponse.getNodes(), requestName);

        // Check final result
        checkFinalResult(error, requestName, tableName);

        // Remove task
        TaskManager.getInstance().removeTask(taskId);
    }
}
