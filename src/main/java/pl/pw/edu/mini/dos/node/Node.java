package pl.pw.edu.mini.dos.node;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitor;
import net.sf.jsqlparser.statement.Statements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Helper;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.ErrorHandler;
import pl.pw.edu.mini.dos.communication.Record;
import pl.pw.edu.mini.dos.communication.Services;
import pl.pw.edu.mini.dos.communication.masternode.*;
import pl.pw.edu.mini.dos.communication.nodemaster.*;
import pl.pw.edu.mini.dos.communication.nodenode.*;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Node extends UnicastRemoteObject
        implements MasterNodeInterface, NodeNodeInterface, Serializable {
    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(Node.class);

    private String data = "new";
    private NodeMasterInterface master;

    public Node() throws RemoteException {
        this("127.0.0.1", "1099", "127.0.0.1");
    }

    public Node(String masterHost, String masterPort, String myIp) throws RemoteException {
        System.setProperty("java.rmi.server.hostname", myIp);   // TODO: It's necessary?

        RMIClient client = new RMIClient(masterHost, Integer.parseInt(masterPort));
        try {
            master = (NodeMasterInterface) client.getService(Services.MASTER);
            master.register(new RegisterRequest(this));
        } catch (RemoteException e) {
            ErrorHandler.handleError(e, true);
        }
    }

    /**
     * @param args = {"localhost", "1099", "localhost"}
     */
    public static void main(String[] args) throws URISyntaxException, RemoteException {
        Node node;
        if(args.length == 3) {
            node = new Node(args[0], args[1], args[2]);
        } else {
            node = new Node();
        }
        Scanner scanner = new Scanner (System.in);
        System.out.println("*Enter 'q' to stop node or 'n' to generate new data.");
        while(scanner.hasNext()) {
            String text = scanner.next();
            if(text.equals("q")) {
                break;
            } else if(text.equals("n")) {
                node.setData(Double.toString(Math.random()));
                logger.info("New data generated: " + node.getData());
            }
        }

        node.stopNode();
        logger.info("Node stopped!");
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void stopNode() {
        master = null;
        System.exit(0); // Unfortunately, this is only way, to close RMI...
    }

    @Override
    public ExecuteSQLOnNodeResponse executeSQLOnNode(ExecuteSQLOnNodeRequest executeSQLOnNodeRequest)
            throws RemoteException {
        try (SqLiteDb db = new SqLiteDb()) {
            Statement stmt = CCJSqlParserUtil.parse(executeSQLOnNodeRequest.getSql());
            SqlLiteStatementVisitor visitor = new SqlLiteStatementVisitor(db, master);
            stmt.accept(visitor);
            return visitor.getResult();
        } catch (JSQLParserException e) {
            logger.error("Sql parsing error: {} - {}", e.getMessage(), e.getStackTrace());
            return new ExecuteSQLOnNodeResponse("", ErrorEnum.SQL_PARSING_ERROR);
        }
    }

    @Override
    public CheckStatusResponse checkStatus(CheckStatusRequest checkStatusRequest) throws RemoteException {
        return null;
    }


    @Override
    public InsertDataResponse insertData(InsertDataRequest insertDataRequest)
            throws RemoteException {
        logger.info("Performing insert: {}", insertDataRequest.getInsertSql());

        try (SqLiteDb db = new SqLiteDb()) {
            Integer rowsAffected = db.ExecuteQuery(insertDataRequest.getInsertSql());
            String response = rowsAffected.toString() + " rows affected";
            return new InsertDataResponse(ErrorEnum.NO_ERROR, response);
        } catch (SQLException e) {
            logger.error("Error executing sql query: {} error: {} stack: {}",
                    insertDataRequest.getInsertSql(),
                    e.getMessage(),
                    e.getStackTrace());
            return new InsertDataResponse(ErrorEnum.ANOTHER_ERROR, e.getMessage());
        }
    }

    @Override
    public SelectDataResponse selectMetadata(SelectDataRequest selectDataRequest)
            throws RemoteException {
        return null;
    }

    @Override
    public UpdateDataResponse updateMetadata(UpdateDataRequest updateDataRequest)
            throws RemoteException {
        return null;
    }

    @Override
    public DeleteDataResponse deleteMetadata(DeleteDataRequest deleteDataRequest)
            throws RemoteException {
        return null;
    }

    @Override
    public TableDataResponse tableMetadata(TableDataRequest tableDataRequest)
            throws RemoteException {
        return null;
    }
}
