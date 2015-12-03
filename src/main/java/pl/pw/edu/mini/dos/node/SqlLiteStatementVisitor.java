package pl.pw.edu.mini.dos.node;

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
import pl.pw.edu.mini.dos.communication.nodemaster.InsertMetadataRequest;
import pl.pw.edu.mini.dos.communication.nodemaster.InsertMetadataResponse;
import pl.pw.edu.mini.dos.communication.nodemaster.NodeMasterInterface;
import pl.pw.edu.mini.dos.communication.nodenode.InsertDataRequest;
import pl.pw.edu.mini.dos.communication.nodenode.InsertDataResponse;
import pl.pw.edu.mini.dos.communication.nodenode.NodeNodeInterface;

import java.rmi.RemoteException;
import java.sql.SQLException;

/**
 * Created by ghash on 02.12.2015.
 */
public class SqlLiteStatementVisitor implements StatementVisitor {
    private static final Logger logger = LoggerFactory.getLogger(SqlLiteStatementVisitor.class);
    private SqLiteDb db;
    private NodeMasterInterface master;
    private ExecuteSQLOnNodeResponse result;

    public SqlLiteStatementVisitor(SqLiteDb db, NodeMasterInterface master) {
        this.db = db;
        this.master = master;
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
        InsertMetadataResponse insertMetadataResponse = null;
        try {
            insertMetadataResponse =
                    master.insertMetadata(new InsertMetadataRequest(tableName));
        } catch (RemoteException e) {
            logger.error("Cannot get insert metadata from master: {}", e.getMessage());
        }

        String response = "";
        // Insert data to nodes pointed by master
        try {
            for (NodeNodeInterface node : insertMetadataResponse.getNodes()) {
                InsertDataRequest insertDataRequest = new InsertDataRequest(insert.toString());
                InsertDataResponse insertDataResponse = node.insertData(insertDataRequest);
                response = insertDataResponse.getResponse();
                if (insertDataResponse.getError() != ErrorEnum.NO_ERROR) {
                    this.result = new ExecuteSQLOnNodeResponse(
                            response, insertDataResponse.getError());
                    logger.error("Cannot insert data to another node: {}", insertDataResponse.getError());
                    return;
                }
            }
        } catch (RemoteException e) {
            logger.error("Cannot insert data to another node: {}", e.getMessage());
        }

        this.result = new ExecuteSQLOnNodeResponse(
                response, ErrorEnum.NO_ERROR);
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
        logger.info("I GOT CREATE TABLE {}", createTable.getTable().getName());
        try {
            db.executeQuery(createTable.toString());
        } catch (SQLException e) {
            if(e.getMessage().equals("table " + createTable.getTable().getName() + " already exists")){
                this.result = new ExecuteSQLOnNodeResponse(
                        "", ErrorEnum.TABLE_ALREADY_EXISTS);
            } else {
                this.result = new ExecuteSQLOnNodeResponse(
                        "", ErrorEnum.ANOTHER_ERROR);
            }
            logger.error("Error executing sql query: {} error: {} stack: {}",
                    e.getErrorCode(),
                    e.getMessage(),
                    e.getStackTrace());
            return;
        }
        this.result = new ExecuteSQLOnNodeResponse(
                createTable.getTable().getName() + " table created", ErrorEnum.NO_ERROR);
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
