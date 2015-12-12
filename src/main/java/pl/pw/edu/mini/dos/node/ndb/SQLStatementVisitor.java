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
import net.sf.jsqlparser.statement.select.WithItem;
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
import java.util.List;
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
        logger.debug("Select: " + select.getSelectBody().toString());

        if (select.getWithItemsList() == null) {
            logger.info("select.getWithItemsList() is empty");
        } else {
            for (WithItem withItem : select.getWithItemsList()) {
                logger.info(withItem.toString());
            }
        }

        // Get tables names
        SQLSelectTablesFinder tablesFinder = new SQLSelectTablesFinder();
        List<String> tables = tablesFinder.getTableList(select);
        logger.info("Tables: " + Helper.collectionToString(tables));

        // Get nodes to select data from
        SelectMetadataResponse selectMetadataResponse;
        try {
            selectMetadataResponse =
                    master.selectMetadata(new SelectMetadataRequest(tables));
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

//        // Create task
//        int numSubTasks = selectMetadataResponse.getNodes().size();
//        TaskManager.getInstance().add(taskId, numSubTasks); // ?
//
//        // Select data from nodes pointed by master
//        List<ExecuteSqlResponse> executeSqlResponses = new LinkedList<>();
//        try {
//            for (NodeNodeInterface node : selectMetadataResponse.getNodes()) {
//                logger.info("Send request to node " + node.toString());
//                ExecuteSqlResponse executeSqlResponse =
//                        node.executeSql(new ExecuteSqlRequest(
//                        taskId, select.toString(), thisNode));
//                executeSqlResponses.add(executeSqlResponse);
//                logger.info("Get request from node {result=" + executeSqlResponse.getResult()
//                        + " data=" + executeSqlResponse.getData() + "}");
//            }
//        } catch (RemoteException e) {
//            logger.error("Cannot select data from another node: {}", e.getMessage());
//            TaskManager.getInstance().updateSubTask(
//                    taskId, ErrorEnum.REMOTE_EXCEPTION, e.getMessage());
//        }
//
//        if (executeSqlResponses.size() == 0) {
//            logger.info("I get no data from another Nodes.");
//            this.result = new ExecuteSQLOnNodeResponse("", ErrorEnum.NO_ERROR);
//            return;
//        }

        logger.info("Select is DONE!");

        // String resultOfQuery = Helper.executeSqlResponseListToString(executeSqlResponses);
        this.result = new ExecuteSQLOnNodeResponse(
                "asdf,asdf,asdf", ErrorEnum.NO_ERROR); // TODO: merger error from all
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

        // Register task and subtasks
        int numSubTasks = insertMetadataResponse.getNodes().size();
        TaskManager.getInstance().add(taskId, numSubTasks);

        // Insert data to nodes pointed by master
        // Send requests
        for (NodeNodeInterface node : insertMetadataResponse.getNodes()) {
            try {
                node.executeSql(new ExecuteSqlRequest(
                        taskId, insert.toString(), thisNode));
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
                TaskManager.getInstance().updateSubTask(
                        taskId, ErrorEnum.REMOTE_EXCEPTION);
            } catch (ExecutionException e) {
                logger.error("Error at getting result: {}", e.getMessage());
                error = ErrorEnum.REMOTE_EXCEPTION;
            } catch (InterruptedException e) {
                logger.error("Operation interrupted", e.getMessage());
                error = ErrorEnum.ANOTHER_ERROR;
            }
            if(response != null && !response.getError().equals(ErrorEnum.NO_ERROR)){
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
        String tableName = createTable.getTable().getName();
        String createStatement = createTable.toString();
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
                        taskId, createTable.toString(), thisNode));
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
                TaskManager.getInstance().updateSubTask(
                        taskId, ErrorEnum.REMOTE_EXCEPTION);
            } catch (ExecutionException e) {
                logger.error("Error at getting result: {}", e.getMessage());
                error = ErrorEnum.REMOTE_EXCEPTION;
            } catch (InterruptedException e) {
                logger.error("Operation interrupted", e.getMessage());
                error = ErrorEnum.ANOTHER_ERROR;
            }
            if(response != null && !response.getError().equals(ErrorEnum.NO_ERROR)){
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
