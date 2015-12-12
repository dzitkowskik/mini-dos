package pl.pw.edu.mini.dos.node;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Config;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.ErrorHandler;
import pl.pw.edu.mini.dos.communication.Services;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLResponse;
import pl.pw.edu.mini.dos.communication.masternode.*;
import pl.pw.edu.mini.dos.communication.nodemaster.NodeMasterInterface;
import pl.pw.edu.mini.dos.communication.nodemaster.RegisterRequest;
import pl.pw.edu.mini.dos.communication.nodenode.*;
import pl.pw.edu.mini.dos.node.ndb.DBmanager;
import pl.pw.edu.mini.dos.node.ndb.SQLStatementVisitor;
import pl.pw.edu.mini.dos.node.rmi.RMIClient;
import pl.pw.edu.mini.dos.node.task.TaskManager;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Node extends UnicastRemoteObject
        implements MasterNodeInterface, NodeNodeInterface, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Node.class);
    private static final Config config = Config.getConfig();

    private NodeMasterInterface master;
    private DBmanager dbManager;
    private ExecutorService workQueue;
    private Map<Long, Future<GetSqlResultResponse>> runningTasks;

    public Node() throws RemoteException {
        this("127.0.0.1", "1099", "127.0.0.1");
    }

    public Node(String masterHost, String masterPort, String myIp) throws RemoteException {
        System.setProperty("java.rmi.server.hostname", myIp);   // TODO: It's necessary?

        dbManager = new DBmanager(false); // Manager for persistent db

        // Create thread pool
        int workerThreads = Integer.parseInt(config.getProperty("nodeWorkerThreads"));
        workQueue = Executors.newFixedThreadPool(workerThreads);
        runningTasks = new HashMap<>();

        // Run services
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
        if (args.length == 3) {
            node = new Node(args[0], args[1], args[2]);
        } else {
            node = new Node();
        }
        Scanner scanner = new Scanner(System.in);
        System.out.println("*Enter 'q' to stop node.");
        while (scanner.hasNext()) {
            String text = scanner.next();
            if (text.equals("q")) {
                break;
            }
        }

        node.stopNode();
        logger.info("Node stopped!");
    }

    public void stopNode() {
        logger.info("Stopping node...");
        workQueue.shutdown();
        while (!workQueue.isTerminated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
        master = null;
        // TODO: REALLY??!! THIS RMI SUCKS!!
        System.exit(0); // Unfortunately, this is only way, to close RMI...
    }

    @Override
    public ExecuteSQLOnNodeResponse executeSQLOnNode(ExecuteSQLOnNodeRequest executeSQLOnNodeRequest)
            throws RemoteException {
        Long taskId = executeSQLOnNodeRequest.getTaskId();
        try {
            Statement stmt = CCJSqlParserUtil.parse(executeSQLOnNodeRequest.getSql());
            SQLStatementVisitor visitor = new SQLStatementVisitor(master, this, taskId);
            stmt.accept(visitor);
            logger.info("Sending response for select: " + visitor.getResult().getResult());
            return visitor.getResult();
        } catch (JSQLParserException e) {
            logger.error("Sql parsing error: {} - {}", e.getMessage(), e.getStackTrace());
            return new ExecuteSQLOnNodeResponse("", ErrorEnum.SQL_PARSING_ERROR);
        }
    }

    @Override
    public ExecuteCreateTablesResponse createTables(
            ExecuteCreateTablesRequest executeCreateTablesRequest) throws RemoteException {
        boolean ok = dbManager.createTables(
                executeCreateTablesRequest.getCreateTableStatements());
        if (!ok) {
            return new ExecuteCreateTablesResponse(ErrorEnum.ANOTHER_ERROR);
        }
        return new ExecuteCreateTablesResponse(ErrorEnum.NO_ERROR);
    }

    @Override
    public CheckStatusResponse checkStatus(CheckStatusRequest checkStatusRequest)
            throws RemoteException {
        Stats stats = new Stats();
        return new CheckStatusResponse(
                stats.getSystemLoad(),
                stats.getDbSize(),
                stats.getFreeMemory());
    }

    @Override
    public ExecuteSqlResponse executeSql(ExecuteSqlRequest request) throws RemoteException {
        logger.info("Got sql to execute: {}", request.getSql());
        // Create and shedule sqlite job to execute
        runningTasks.put(request.getTaskId(),
                workQueue.submit(dbManager.newSQLJob(request)));
        return new ExecuteSqlResponse();
    }

    @Override
    public GetSqlResultResponse getSqlResult(GetSqlResultRequest request)
            throws RemoteException, ExecutionException, InterruptedException {
        // Get runing task
        Future<GetSqlResultResponse> task = runningTasks.get(request.getTaskId());
        // Get result
        GetSqlResultResponse response = task.get();
        runningTasks.remove(request.getTaskId());
        return response;
    }

    @Override
    public AskToCommitResponse askToCommit(AskToCommitRequest request) throws RemoteException {
        // Update status of the subtask
        TaskManager.getInstance().updateSubTask(request.getTaskId(), request.getErrorType());
        // Wait untill all subtasks are done
        boolean error = TaskManager.getInstance().waitForCompletion(request.getTaskId());
        // Decide if order to commit or rollback
        if (error) {
            logger.info("Coordinator: order to rollback");
            return new AskToCommitResponse(false);
        } else {
            logger.info("Coordinator: order to commit");
            return new AskToCommitResponse(true);
        }
    }
}
