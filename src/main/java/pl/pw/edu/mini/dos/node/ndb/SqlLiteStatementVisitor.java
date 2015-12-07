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
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.masternode.ExecuteSQLOnNodeResponse;
import pl.pw.edu.mini.dos.communication.nodemaster.*;
import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlRequest;
import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlResponse;
import pl.pw.edu.mini.dos.communication.nodenode.NodeNodeInterface;
import pl.pw.edu.mini.dos.node.task.TaskManager;

import java.rmi.RemoteException;

public class SqlLiteStatementVisitor implements StatementVisitor {
    private static final Logger logger = LoggerFactory.getLogger(SqlLiteStatementVisitor.class);
    private NodeMasterInterface master;
    private NodeNodeInterface thisNode;
    private ExecuteSQLOnNodeResponse result;
    private Long taskId;

    public SqlLiteStatementVisitor(
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

    }

    @Override
    public void visit(Delete delete) {

    }

    @Override
    public void visit(Update update) {

    }

    @Override
    public void visit(Insert insert) {
        logger.info("I GOT INSERT TO TABLE {}", insert.getTable().getName());

        // Get table name
        String tableName = insert.getTable().getName();

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

        int numSubTasks = insertMetadataResponse.getNodes().size();
        TaskManager.getInstance().add(taskId, numSubTasks);

        // Insert data to nodes pointed by master
        try {
            for (NodeNodeInterface node : insertMetadataResponse.getNodes()) {
                node.executeSql(new ExecuteSqlRequest(
                        taskId, insert.toString(), thisNode));
            }
        } catch (RemoteException e) {
            logger.error("Cannot insert data to another node: {}", e.getMessage());
            TaskManager.getInstance().updateSubTask(
                    taskId, ErrorEnum.REMOTE_EXCEPTION, e.getMessage());
        }

        ExecuteSqlResponse response = TaskManager.getInstance().waitForCompletion(taskId);
        logger.info("Insert is DONE!");

        this.result = new ExecuteSQLOnNodeResponse(response.getResult(), response.getError());
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
        String tableName = createTable.getTable().getName();
        String createStatement = createTable.toString(); // TODO check if ; is needed
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

        int numSubTasks = createMetadataResponse.getNodes().size();
        TaskManager.getInstance().add(taskId, numSubTasks);

        // Create table in nodes pointed by master
        try {
            for (NodeNodeInterface node : createMetadataResponse.getNodes()) {
                node.executeSql(new ExecuteSqlRequest(
                        taskId, createTable.toString(), thisNode));
            }
        } catch (RemoteException e) {
            logger.error("Cannot create table in another node: {}", e.getMessage());
            TaskManager.getInstance().updateSubTask(
                    taskId, ErrorEnum.REMOTE_EXCEPTION, e.getMessage());
        }

        ExecuteSqlResponse response = TaskManager.getInstance().waitForCompletion(taskId);
        logger.info("Create table is DONE!");

        this.result = new ExecuteSQLOnNodeResponse(response.getResult(), response.getError());
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
