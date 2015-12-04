package pl.pw.edu.mini.dos.node;

import javafx.util.Pair;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Config;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.ErrorHandler;
import pl.pw.edu.mini.dos.communication.Services;
import pl.pw.edu.mini.dos.communication.masternode.*;
import pl.pw.edu.mini.dos.communication.nodemaster.NodeMasterInterface;
import pl.pw.edu.mini.dos.communication.nodemaster.RegisterRequest;
import pl.pw.edu.mini.dos.communication.nodenode.*;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

public class Node extends UnicastRemoteObject
        implements MasterNodeInterface, NodeNodeInterface, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Node.class);
    private static final Config config = Config.getConfig();

    private WorkQueue workQueue;
    private NodeMasterInterface master;

    public Node() throws RemoteException {
        this("127.0.0.1", "1099", "127.0.0.1");
    }

    public Node(String masterHost, String masterPort, String myIp) throws RemoteException {
        System.setProperty("java.rmi.server.hostname", myIp);   // TODO: It's necessary?

        // CREATE THREAD POOL
        int workerThreads = Integer.parseInt(config.getProperty("nodeWorkerThreads"));
        workQueue = new WorkQueue(workerThreads);

        // RUN SERVICES
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
            }
        }

        node.stopNode();
        logger.info("Node stopped!");
    }

    public void stopNode() {
        master = null;
        // TODO: REALLY??!! THIS RMI SUCKS!!
        System.exit(0); // Unfortunately, this is only way, to close RMI...
    }

    @Override
    public ExecuteSQLOnNodeResponse executeSQLOnNode(ExecuteSQLOnNodeRequest executeSQLOnNodeRequest)
            throws RemoteException {
        Long taskId = executeSQLOnNodeRequest.getTaskId();
        try (SqLiteDb db = new SqLiteDb()) {
            Statement stmt = CCJSqlParserUtil.parse(executeSQLOnNodeRequest.getSql());
            SqlLiteStatementVisitor visitor = new SqlLiteStatementVisitor(db, master, this, taskId);
            stmt.accept(visitor);
            return visitor.getResult();
        } catch (JSQLParserException e) {
            logger.error("Sql parsing error: {} - {}", e.getMessage(), e.getStackTrace());
            return new ExecuteSQLOnNodeResponse("", ErrorEnum.SQL_PARSING_ERROR);
        }
    }

    @Override
    public CheckStatusResponse checkStatus(CheckStatusRequest checkStatusRequest) throws RemoteException {
        Stats stats = new Stats();
        return new CheckStatusResponse(
                stats.getSystemLoad(),
                stats.getDbSize(),
                stats.getFreeMemory());
    }

    @Override
    public ExecuteSqlResponse executeSql(ExecuteSqlRequest request) throws RemoteException {
        logger.info("Got sql to execute: {}", request.sql);

        // Create and shedule sqlite job to execute
        SQLiteJob job = new SQLiteJob(request);
        workQueue.execute(job);

        return new ExecuteSqlResponse(ErrorEnum.NO_ERROR, "");
    }

    @Override
    public AskToCommitResponse askToCommit(AskToCommitRequest request) throws RemoteException {
        logger.info("askToCommit start");

        TaskCompletion.getInstance().update(
                request.taskId,
                request.errorType,
                request.errorMessage,
                request.queryResult);

        Pair<ErrorEnum, String> result =
            TaskCompletion.getInstance().waitForCompletion(request.taskId);

        logger.info("askToCommit end");
        if(result.getKey() == ErrorEnum.NO_ERROR) {
            return new AskToCommitResponse(true);
        } else {
            return new AskToCommitResponse(false);
        }
    }
}
