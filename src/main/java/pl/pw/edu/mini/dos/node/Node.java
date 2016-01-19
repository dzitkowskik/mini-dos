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
import pl.pw.edu.mini.dos.node.ndb.ImDBmanager;
import pl.pw.edu.mini.dos.node.ndb.SQLStatementVisitor;
import pl.pw.edu.mini.dos.node.rmi.RMIClient;
import pl.pw.edu.mini.dos.node.task.TaskManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
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
    DBmanager dbManager;
    private ExecutorService workQueue;
    private Map<Long, Future<GetSqlResultResponse>> runningTasks;
    private String dbName;
    private boolean stop = false;

    public Node() throws RemoteException {
        this("127.0.0.1", "1099", "127.0.0.1", Node.randomDbName());
    }

    public Node(String dbName) throws RemoteException {
        this("127.0.0.1", "1099", "127.0.0.1", dbName);
    }
    public Node(String masterHost, String masterPort, String myIp) throws RemoteException {
        this(masterHost, masterPort, myIp, Node.randomDbName());
    }

    public Node(String masterHost, String masterPort, String myIp, String dbName) throws RemoteException {
        System.setProperty("java.rmi.server.hostname", myIp);

        this.dbName = dbName;
        dbManager = new DBmanager(this.dbName); // Manager for persistent db

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
     * @param args = {"localhost", "1099", "localhost", "dbName"}
     */
    public static void main(String[] args) {
        Node node;
        try {
            if (args.length == 4) {
                node = new Node(args[0], args[1], args[2], args[3]);
            } else if (args.length == 3) {
                node = new Node(args[0], args[1], args[2]);
            } else if (args.length == 1) {
                node = new Node(args[0]);
            } else {
                node = new Node();
            }
        } catch (RemoteException e) {
            logger.error(e.getMessage());
            return;
        }

        BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("+===============================+");
        System.out.println("|             N O D E           |");
        System.out.println("+===============================+");
        System.out.println("| Menu:                         |");
        System.out.println("|    'q' -> Stop node           |");
        System.out.println("+===============================+");
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
        logger.trace("Node stopped!");
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

    private static String randomDbName(){
        Random r = new Random();
        int random = r.nextInt(1000);
        return random + "node.db";
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
            logger.trace("Sending result of query: " + visitor.getResult().getResult());
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
        Stats stats = new Stats(dbName);
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

    @SuppressWarnings("ConstantConditions")
    @Override
    public ReplicateDataResponse replicateData(ReplicateDataRequest replicateDataRequest)
            throws RemoteException {
        logger.trace("Replicate data request");
        // Get the data
        List<String> tablesNames = new ArrayList<>(replicateDataRequest.getTablesRows().keySet());

        // Create in-memory db
        ImDBmanager imDbManager = new ImDBmanager();
        // Create needed tables
        imDbManager.createTables(replicateDataRequest.getCreateTableStatements());

        // For each table
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
                    return new ReplicateDataResponse(ErrorEnum.REMOTE_EXCEPTION);
                }
            }
            // Get responses and proccess data
            List<String> versionsOfTable = new ArrayList<>(replicateDataRequest.getTableNodes().size());
            for (int i = 0; i < replicateDataRequest.getTableNodes().get(table).size(); i++) {
                logger.debug("Getting response from node " + i);
                NodeNodeInterface node = replicateDataRequest.getTableNodes().get(table).get(i);
                GetSqlResultResponse response;
                try {
                    response = node.getSqlResult(new GetSqlResultRequest(replicateDataRequest.getTaskId()));
                } catch (RemoteException e) {
                    logger.error("Cannot get data from another node: {}", e.getMessage());
                    return new ReplicateDataResponse(ErrorEnum.REMOTE_EXCEPTION);
                } catch (ExecutionException e) {
                    logger.error("Error at getting result: {}", e.getMessage());
                    return new ReplicateDataResponse(ErrorEnum.REMOTE_EXCEPTION);
                } catch (InterruptedException e) {
                    logger.error("Operation interrupted", e.getMessage());
                    return new ReplicateDataResponse(ErrorEnum.ANOTHER_ERROR);
                }
                if (response == null || !response.getError().equals(ErrorEnum.NO_ERROR)) {
                    return new ReplicateDataResponse(response == null ?
                            ErrorEnum.ANOTHER_ERROR : response.getError());
                } else {
                    // Import received table to in-memory db
                    String versionOftable = table + "_v" + i;
                    versionsOfTable.add(versionOftable);
                    boolean ok = imDbManager.importTable(versionOftable, response.getData());
                    if (!ok) {
                        logger.error("Error at importing table to imdb");
                        return new ReplicateDataResponse(ErrorEnum.ANOTHER_ERROR);
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
                return new ReplicateDataResponse(ErrorEnum.ANOTHER_ERROR);
            }
        }

        // Run selects in temporal tables and insert the received data in the node
        for (String table : tablesNames) {
            List<Long> rows = replicateDataRequest.getTablesRows().get(table);
            String select = "" +
                    "SELECT * " +
                    "FROM " + table + "_tmp " +
                    "WHERE row_id IN (" + rows.get(0);
            for (int i = 1; i < rows.size(); i++) {
                select += ", " + rows.get(i);
            }
            select += ");";
            // Select data
            logger.debug("Collenting data that node had from table " + table);
            SerializableResultSet rs = imDbManager.executeSelectRaw(select);
            // Insert data
            logger.debug("Inserting data that node had from table " + table + " in new node");
            boolean ok = dbManager.insertResultSet(table, rs);
            if (!ok) {
                return new ReplicateDataResponse(ErrorEnum.SQL_EXECUTION_ERROR);
            }
        }

        // Close in-memory db
        imDbManager.close();

        return new ReplicateDataResponse(ErrorEnum.NO_ERROR);
    }

    @Override
    public UpdateTablesResponse updateTables(UpdateTablesRequest updateTablesRequest) throws RemoteException {
        boolean ok = dbManager.createTables(updateTablesRequest.getCreateTableStatements());
        return new UpdateTablesResponse(ok ? ErrorEnum.NO_ERROR : ErrorEnum.ANOTHER_ERROR);
    }

    @Override
    public UpdateMasterResponse updateMaster(UpdateMasterRequest updateMasterRequest) throws RemoteException {
        this.master = updateMasterRequest.getMasterInterface();
        return new UpdateMasterResponse(ErrorEnum.NO_ERROR);
    }

    @Override
    public ExecuteSqlResponse executeSql(ExecuteSqlRequest request) throws RemoteException {
        logger.trace("Got sql to execute: {}", request.getSql());

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
        logger.trace("askToCommit method start...");
        // Update status of the subtask
        TaskManager.getInstance().updateSubTask(request.getTaskId(), request.getErrorType());
        // Wait untill all subtasks are done
        boolean error = TaskManager.getInstance().waitForCompletion(request.getTaskId());
        // Decide if order to commit or rollback
        if (error) {
            logger.trace("Coordinator: order to rollback");
            return new AskToCommitResponse(false);
        } else {
            logger.trace("Coordinator: order to commit");
            return new AskToCommitResponse(true);
        }
    }
}
