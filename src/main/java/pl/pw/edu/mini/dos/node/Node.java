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
import pl.pw.edu.mini.dos.communication.masternode.*;
import pl.pw.edu.mini.dos.communication.nodemaster.NodeMasterInterface;
import pl.pw.edu.mini.dos.communication.nodemaster.RegisterRequest;
import pl.pw.edu.mini.dos.communication.nodenode.*;
import pl.pw.edu.mini.dos.node.ndb.DBmanager;
import pl.pw.edu.mini.dos.node.ndb.InDBmanager;
import pl.pw.edu.mini.dos.node.ndb.SQLStatementVisitor;
import pl.pw.edu.mini.dos.node.rmi.RMIClient;
import pl.pw.edu.mini.dos.node.task.TaskManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Node extends UnicastRemoteObject
        implements MasterNodeInterface, NodeNodeInterface, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Node.class);
    private static final Config config = Config.getConfig();

    NodeMasterInterface master;
    private DBmanager dbManager;
    private ExecutorService workQueue;
    private Map<Long, Future<GetSqlResultResponse>> runningTasks;
    private int dbPrefix;
    private boolean stop = false;

    public Node() throws RemoteException {
        this("127.0.0.1", "1099", "127.0.0.1");
    }

    public Node(String masterHost, String masterPort, String myIp) throws RemoteException {
        System.setProperty("java.rmi.server.hostname", myIp);   // TODO: It's necessary?

        Random r = new Random();
        dbPrefix = r.nextInt(1000);
        dbManager = new DBmanager(dbPrefix); // Manager for persistent db

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
    public static void main(String[] args) {
        Node node;
        try {
            if (args.length == 3) {
                node = new Node(args[0], args[1], args[2]);
            } else {
                node = new Node();
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage());
            return;
        }

        BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("*Enter 'q' to stop node.");
        try {
            while (!node.isStop()) {
                if (scanner.ready()) {
                    String text = scanner.readLine();
                    if (text.equals("q")) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        node.stopNode();
        logger.info("Node stopped!");
    }

    public void stopNode() {
        logger.debug("Stopping node...");
        workQueue.shutdown();
        while (!workQueue.isTerminated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
        master = null;
        try {
            unexportObject(this, true);
        } catch (NoSuchObjectException e) {
            logger.error(e.getMessage());
        }
    }

    public boolean isStop() {
        return stop;
    }

    @Override
    public ExecuteSQLOnNodeResponse executeSQLOnNode(ExecuteSQLOnNodeRequest executeSQLOnNodeRequest)
            throws RemoteException {
        Long taskId = executeSQLOnNodeRequest.getTaskId();
        try {
            Statement stmt = CCJSqlParserUtil.parse(executeSQLOnNodeRequest.getSql());
            SQLStatementVisitor visitor = new SQLStatementVisitor(master, this, taskId);
            stmt.accept(visitor);
            logger.info("Sending result of query: " + visitor.getResult().getResult());
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
        Stats stats = new Stats(dbPrefix);
        return new CheckStatusResponse(
                stats.getSystemLoad(),
                stats.getDbSize(),
                stats.getFreeMemory());
    }

    @Override
    public KillNodeResponse killNode(KillNodeRequest killNodeRequest) throws RemoteException {
        this.stop = true;
        return new KillNodeResponse();
    }

    @Override
    public ReplicateDataResponse replicateData(ReplicateDataRequest replicateDataRequest)
            throws RemoteException {
        // Get the data
        List<String> tablesNames = new ArrayList<>(replicateDataRequest.getTablesRows().keySet());

        // Create in-memory db
        InDBmanager inDbManager = new InDBmanager();
        // Create needed tables
        inDbManager.createTables(replicateDataRequest.getCreateTableStatements());

        // For each table
        ErrorEnum error = ErrorEnum.NO_ERROR;
        Map<String, List<String>> versionsOfTables = new HashMap<>(tablesNames.size());
        Map<String, List<String>> tablesColumnsNames = new HashMap<>(tablesNames.size());
        for (String table : tablesNames) {
            // Send requests to get the needed data
            String selectAll = "SELECT * FROM " + table + ";";
            logger.debug("Sending SELECT * to " +
                    replicateDataRequest.getTableNodes().get(table).size() + " nodes");
            for (NodeNodeInterface node : replicateDataRequest.getTableNodes().get(table)) {
                try {
                    node.executeSql(new ExecuteSqlRequest(
                            replicateDataRequest.getTaskId(), selectAll, this));
                } catch (RemoteException e) {
                    logger.error("Cannot get data from another node: " + e.getMessage());
                    error = ErrorEnum.REMOTE_EXCEPTION;
                }
            }
            // Get responses and proccess data
            List<String> versionsOfTable = new ArrayList<>(replicateDataRequest.getTableNodes().size());
            for (int i = 0; i < replicateDataRequest.getTableNodes().get(table).size(); i++) {
                logger.debug("Getting response from node " + i);
                NodeNodeInterface node = replicateDataRequest.getTableNodes().get(table).get(i);
                GetSqlResultResponse response = null;
                try {
                    response = node.getSqlResult(new GetSqlResultRequest(replicateDataRequest.getTaskId()));
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
                    boolean ok = inDbManager.importTable(versionOftable,
                            response.getData().getColumnsTypes(),
                            response.getData().getColumnsNames(),
                            response.getData().getData());
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
            boolean ok = inDbManager.mergeVersionsOfTable(table,
                    versionsOfTables.get(table), tablesColumnsNames.get(table));
            if (!ok) {
                logger.error("Error at merging versions of table");
                error = ErrorEnum.ANOTHER_ERROR;
            }
        }

        return null;
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
