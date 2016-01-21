package pl.pw.edu.mini.dos.master;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Config;
import pl.pw.edu.mini.dos.Helper;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.Services;
import pl.pw.edu.mini.dos.communication.clientmaster.ClientMasterInterface;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLRequest;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLResponse;
import pl.pw.edu.mini.dos.communication.masternode.*;
import pl.pw.edu.mini.dos.communication.nodemaster.*;
import pl.pw.edu.mini.dos.communication.nodenode.NodeNodeInterface;
import pl.pw.edu.mini.dos.master.mdb.DBmanager;
import pl.pw.edu.mini.dos.master.node.NodeManager;
import pl.pw.edu.mini.dos.master.node.PingNodes;
import pl.pw.edu.mini.dos.master.node.RegisteredNode;
import pl.pw.edu.mini.dos.master.rmi.RMIServer;
import pl.pw.edu.mini.dos.master.task.TaskManager;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Master
        extends UnicastRemoteObject
        implements NodeMasterInterface, ClientMasterInterface, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Master.class);
    static final Config config = Config.getConfig();
    private RMIServer server;

    Thread pingThread;
    NodeManager nodeManager;
    private TaskManager taskManager;
    private DBmanager dbManager;
    private int maxTaskRetryAttempts;


    public Master() throws RemoteException {
        this("127.0.0.1", 1099);
    }

    public Master(String host, int port) throws RemoteException {
        // Get managers
        nodeManager = new NodeManager(
                Integer.parseInt(config.getProperty("replicationFactor")));
        taskManager = new TaskManager();
        dbManager = new DBmanager();

        // Prepare in-memory db
        dbManager.prepareDB();

        // RMI server
        server = new RMIServer(host, port);
        server.startService(Services.MASTER, this);
        logger.trace("Master listening at (" + host + ":" + port + ")");

        // Ping nodes periodically
        long spanTime = Long.parseLong(config.getProperty("spanPingingTime"));
        pingThread = new Thread(new PingNodes(this, nodeManager, spanTime));
        pingThread.start();

        // Set max task retry attemps
        maxTaskRetryAttempts = Integer.parseInt(config.getProperty("maxTaskRetryAttempts"));
    }

    /**
     * @param args = [ipAddress, port]
     */
    public static void main(String[] args) throws RemoteException {
        Master master;
        if (args.length == 2) {
            master = new Master(args[0], Integer.valueOf(args[1]));
        } else {
            master = new Master();
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("+===============================+");
        System.out.println("|          M A S T E R          |");
        System.out.println("+===============================+");
        System.out.println("| Menu:                         |");
        System.out.println("|    'q' -> Stop master         |");
        System.out.println("|    'b' -> Create backup       |");
        System.out.println("|    'r' -> Restore backup      |");
        System.out.println("|                               |");
        System.out.println("|     select (*|n) from nodes;  |");
        System.out.println("|     kill   (*|n) from nodes;  |");
        System.out.println("+===============================+");
        System.out.print("ddbms> ");

        whileLabel:
        while (scanner.hasNext()) {
            String text = scanner.nextLine().toLowerCase();
            switch (text) {
                case "q":
                    scanner.close();
                    break whileLabel;
                case "b":
                    master.createMasterBackup();
                    break;
                case "r":
                    master.restoreMasterBackup();
                    break;
                default:
                    master.parseCommand(text);
                    break;
            }
            System.out.print("ddbms> ");
        }

        master.stopMaster();
        logger.trace("Master stopped!");
    }

    public void stopMaster() {
        pingThread.interrupt();
        server.stopService(Services.MASTER, this);
        dbManager.close();
    }

    /**
     * Parse given command and print result.
     *
     * @param command command
     */
    private void parseCommand(String command) {
        logger.debug("Parse command: " + command);
        Pattern selectTask = Pattern.compile("^(select|kill) (\\*|\\d) from (tasks|nodes);?$");
        Matcher matcher = selectTask.matcher(command);
        if (matcher.find()) {
            if (matcher.group(3).equals("tasks")) {
                // Tasks
                if (matcher.group(1).equals("select")) {
                    if (matcher.group(2).equals("*")) {
                        String result = taskManager.select();
                        System.out.println(result.equals("") ? "0 tasks" : result);
                    } else {
                        String result = taskManager.select(Long.parseLong(matcher.group(2)));
                        System.out.println(result.equals("") ? "0 tasks" : result);
                    }
                }
            } else {
                // Nodes
                if (matcher.group(1).equals("select")) {
                    if (matcher.group(2).equals("*")) {
                        String result = nodeManager.select();
                        System.out.println(result.equals("") ? "0 nodes" : result);
                    } else {
                        String result = nodeManager.select(Integer.parseInt(matcher.group(2)));
                        System.out.println(result.equals("") ? "0 nodes" : result);
                    }
                } else if (matcher.group(1).equals("kill")) {
                    if (matcher.group(2).equals("*")) {
                        System.out.println(nodeManager.kill());
                    } else {
                        System.out.println(nodeManager.kill(Integer.parseInt(matcher.group(2))));
                    }
                }
            }
        }
    }

    public void createMasterBackup() {
        dbManager.createBackup();
        taskManager.createBackup();
        nodeManager.createBackup();
        System.out.println("Backup created!");
    }

    public void restoreMasterBackup() {
        dbManager.restoreBackup();
        taskManager.restoreBackup();
        nodeManager.restoreBackup();
        for(MasterNodeInterface node : nodeManager.getNodesInterfaces()){
            try {
                node.updateMaster(new UpdateMasterRequest(this));
            } catch (RemoteException e) {
                logger.error("Error at updating master interface in nodes");
                return;
            }
        }
        System.out.println("Backup restored!");
    }

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) throws RemoteException {
        // Register node
        ErrorEnum ok = nodeManager.newNode(registerRequest.getNodeInterface());
        if (!ok.equals(ErrorEnum.NO_ERROR)) {
            return new RegisterResponse(ok);
        }
        // Send node create tables
        ExecuteCreateTablesResponse response =
                registerRequest.getNodeInterface().createTables
                        (new ExecuteCreateTablesRequest(dbManager.getCreateTableStatements()));
        return new RegisterResponse(response.getError());
    }

    @Override
    public InsertMetadataResponse insertMetadata(InsertMetadataRequest insertMetadataRequest)
            throws RemoteException {
        // Select nodes
        List<RegisteredNode> nodes = nodeManager.selectNodesInsert();
        if (nodes == null) {
            // Number of available nodes less than replication factor
            logger.error("Cannot inert data, not enought nodes available.");
            return new InsertMetadataResponse(null, null, ErrorEnum.NOT_ENOUGH_NODES);
        }
        // Insert row metadata (RowId, TableId, NodesIds)
        List<Integer> nodesIds = new ArrayList<>(nodes.size());
        List<NodeNodeInterface> nodesInterfaces = new ArrayList<>(nodes.size());
        for (RegisteredNode node : nodes) {
            nodesIds.add(node.getID());
            nodesInterfaces.add((NodeNodeInterface) node.getInterface());
        }
        Long rowId = dbManager.insertRow(
                insertMetadataRequest.getTable(), nodesIds);
        if (rowId == null) {
            // The table of the insert doesn't exist
            return new InsertMetadataResponse(null, null, ErrorEnum.TABLE_NOT_EXIST);
        }
        return new InsertMetadataResponse(rowId, nodesInterfaces, ErrorEnum.NO_ERROR);
    }

    @Override
    public SelectMetadataResponse selectMetadata(SelectMetadataRequest selectMetadataRequest)
            throws RemoteException {
        // Get create tables and node that have the data
        List<String> tables = selectMetadataRequest.getTables();
        logger.trace("Get metadata select request for tables: " + Helper.collectionToString(tables));
        if (tables == null || tables.size() == 0) {
            return new SelectMetadataResponse(null, null, ErrorEnum.ANOTHER_ERROR);
        }
        List<String> createTableStatements = dbManager.getCreateTableStatements(tables);
        Map<String, List<NodeNodeInterface>> tableNodesInterfaces = new HashMap<>();
        for (String table : tables) {
            List<Integer> nodesIDs = dbManager.getNodesHaveTable(table);
            if (createTableStatements == null || nodesIDs == null) {
                return new SelectMetadataResponse(null, null, ErrorEnum.TABLE_NOT_EXIST);
            }
            logger.trace("Nodes which have table " + table + ": " + Helper.collectionToString(nodesIDs));
            List<NodeNodeInterface> nodesInterfaces = new ArrayList<>(nodesIDs.size());
            for (Integer nodeID : nodesIDs) {
                NodeNodeInterface nodeInterface = (NodeNodeInterface) nodeManager.getNodeInterface(nodeID);
                if (nodeInterface != null) {
                    nodesInterfaces.add(nodeInterface);
                }
            }
            tableNodesInterfaces.put(table, nodesInterfaces);
        }
        return new SelectMetadataResponse(tableNodesInterfaces,
                createTableStatements, ErrorEnum.NO_ERROR);
    }

    @Override
    public UpdateMetadataResponse updateMetadata(UpdateMetadataRequest updateMetadataRequest)
            throws RemoteException {
        // Get nodes that have data from specified tables
        Set<Integer> nodesIDs = new HashSet<>();
        for (String table : updateMetadataRequest.getTables()) {
            nodesIDs.addAll(dbManager.getNodesHaveTable(table));
        }

        if (nodesIDs.isEmpty()) {
            return new UpdateMetadataResponse(null, ErrorEnum.TABLE_NOT_EXIST);
        }

        logger.trace("Nodes which have the data: " + Helper.collectionToString(nodesIDs));
        List<NodeNodeInterface> nodesInterfaces = new ArrayList<>(nodesIDs.size());
        for (Integer nodeID : nodesIDs) {
            NodeNodeInterface nodeInterface = (NodeNodeInterface) nodeManager.getNodeInterface(nodeID);
            if (nodeInterface != null) {
                nodesInterfaces.add(nodeInterface);
            }
        }

        return new UpdateMetadataResponse(nodesInterfaces, ErrorEnum.NO_ERROR);
    }

    @Override
    public DeleteMetadataResponse deleteMetadata(DeleteMetadataRequest deleteMetadataRequest)
            throws RemoteException {
        // Get nodes that have data from specified table
        List<Integer> nodesIDs = dbManager.getNodesHaveTable(deleteMetadataRequest.getTable());
        if (nodesIDs == null) {
            return new DeleteMetadataResponse(null, ErrorEnum.TABLE_NOT_EXIST);
        }
        logger.trace("Nodes which have the data: " + Helper.collectionToString(nodesIDs));
        List<NodeNodeInterface> nodesInterfaces = new ArrayList<>(nodesIDs.size());
        for (Integer nodeID : nodesIDs) {
            NodeNodeInterface nodeInterface = (NodeNodeInterface) nodeManager.getNodeInterface(nodeID);
            if (nodeInterface != null) {
                nodesInterfaces.add(nodeInterface);
            }
        }

        return new DeleteMetadataResponse(nodesInterfaces, ErrorEnum.NO_ERROR);
    }

    @SuppressWarnings("unchecked")
    @Override
    public TableMetadataResponse tableMetadata(TableMetadataRequest tableMetadataRequest)
            throws RemoteException {
        logger.trace("tableMetadata start, table = " +
                tableMetadataRequest.getTable());
        List<NodeNodeInterface> nodesInterfaces =
                (List<NodeNodeInterface>) (List<?>) nodeManager.getNodesInterfaces();
        return new TableMetadataResponse(
                nodesInterfaces,
                dbManager.insertTable(
                        tableMetadataRequest.getTable(),
                        tableMetadataRequest.getTableStatement()));
    }

    @Override
    public ExecuteSQLResponse executeSQL(ExecuteSQLRequest executeSQLRequest) throws RemoteException {
        boolean finish = false;
        int numRetry = 1;
        ExecuteSQLOnNodeResponse result = null;
        do {
            RegisteredNode node = nodeManager.selectCoordinatorNode();
            if(node == null) {
                // 0 nodes available
                logger.error("No nodes avaible");
                result = new ExecuteSQLOnNodeResponse("", ErrorEnum.NO_NODES_AVAILABLE);
                break;
            }
            Long taskID = taskManager.newTask(node.getID());
            logger.trace("New task " + taskID + ". Coordinator node: " + node.getID());
            try {
                result = node.getInterface().executeSQLOnNode(
                        new ExecuteSQLOnNodeRequest(taskID, executeSQLRequest.getSql()));
            } catch (RemoteException e) {
                logger.error("Cannot execute sql in node");
                continue;
            }
            switch (result.getError()) {
                case NO_ERROR:
                    logger.trace("Task finised");
                    taskManager.setFinishedTask(taskID);
                    finish = true;
                    break;
                case SQL_EXECUTION_ERROR:
                case SQL_PARSING_ERROR:
                case TABLE_NOT_EXIST:
                    logger.error("Task aborted: user error");
                    taskManager.setAbortedTask(taskID);
                    finish = true;
                    break;
                default:
                    taskManager.setAbortedTask(taskID);
                    if (numRetry < maxTaskRetryAttempts) {
                        ++numRetry;
                        logger.error("Task aborted. Trying " + numRetry + " attempt...");
                    } else {
                        logger.error("Task aborted. Max retry attempts exceeded.");
                        finish = true;
                    }
            }
        } while (!finish);
        logger.trace("Send response to client:" + result.getResult());
        return new ExecuteSQLResponse(result);
    }

    /**
     * This method is called when the max number of retry ping attempts to a node is exceeded.
     * The data that this node had is replicated to mantein the replication factor and the
     * node is unregister from master.
     *
     * @param node node
     */
    @SuppressWarnings("ConstantConditions")
    public void unregisterNode(RegisteredNode node) {
        logger.trace("Unregistering node " + node.getID());
        // Get tables and rowsIDs that node had
        Map<String, List<Long>> tablesRows = dbManager.getDataNodeHas(node);
        // Unregister node in master
        nodeManager.unregisterNode(node);
        dbManager.removeRecordsOfNode(node);
        // Send task to replicate data
        List<RegisteredNode> newNode = nodeManager.selectNodesInsert();
        if(newNode != null){
            replicateDataToNode(tablesRows, newNode.get(0));
        } else {
            logger.error("Data of node " + node.getID()
                    + " has not been replicated. Not enough nodes available.");
        }
    }

    /**
     * Send a request to a node to update all the tables it has. Master sends node
     * all create tables statements and node create the tables it doesn't have.
     *
     * @param node node to update tables
     */
    @SuppressWarnings("ConstantConditions")
    public void updateTablesNode(RegisteredNode node) {
        logger.trace("Update tables of node " + node.getID());
        // Send request to update the tables of node
        List<String> createTables = dbManager.getCreateTableStatements();
        UpdateTablesResponse response = null;
        try {
            response = node.getInterface().updateTables(new UpdateTablesRequest(createTables));
        } catch (RemoteException e) {
            logger.error("Cannot update tables: {}", e.getMessage());
        }
        if (response.getError().equals(ErrorEnum.NO_ERROR)) {
            logger.trace("Tables from node " + node.getID() + " was updated successfully.");
        } else {
            logger.error("Error at updating tables: " + response.getError());
        }
    }

    /**
     * Replicate given data to a new node.
     *
     * @param data    map table -> rowsIDs
     * @param newNode node where to replicate the data
     * @return true if no errors
     */
    private boolean replicateDataToNode(Map<String, List<Long>> data, RegisteredNode newNode) {
        logger.debug("Replicate data that node had");
        // Start replicate data task
        Long taskID = taskManager.newTask(newNode.getID());
        // Get nodes which have the data
        logger.debug("Tables: " + Helper.collectionToString(data.keySet()));
        Map<String, List<NodeNodeInterface>> tableNodesInterfaces = new HashMap<>();
        for (String table : data.keySet()) {
            List<Integer> nodesIDs = dbManager.getNodesHaveTable(table);
            logger.debug("Nodes which have table " + table + ": " + Helper.collectionToString(nodesIDs));
            List<NodeNodeInterface> nodesInterfaces = new ArrayList<>(nodesIDs.size());
            for (Integer nodeID : nodesIDs) {
                NodeNodeInterface nodeInterface = (NodeNodeInterface) nodeManager.getNodeInterface(nodeID);
                if (nodeInterface != null) {
                    nodesInterfaces.add(nodeInterface);
                }
            }
            tableNodesInterfaces.put(table, nodesInterfaces);
        }
        ReplicateDataResponse response;
        logger.debug("Replicate data in " + newNode.getID());
        try {
            response = newNode.getInterface().replicateData(
                    new ReplicateDataRequest(taskID, tableNodesInterfaces, data,
                            dbManager.getCreateTableStatements(new ArrayList<>(data.keySet()))));
        } catch (RemoteException e) {
            logger.error("Cannot replicate data: {}", e.getMessage());
            return false;
        }
        if (!response.getError().equals(ErrorEnum.NO_ERROR)) {
            logger.error("Error at replicating data: " + response.getError());
            return false;
        }
        // Insert row metadata (RowId, TableId, NodesIds)
        List<Integer> nodesIds = new ArrayList<>();
        nodesIds.add(newNode.getID());
        for (String table : data.keySet()) {
            for (Long rowId : data.get(table)) {
                dbManager.insertRow(rowId, table, nodesIds);
            }
        }
        logger.trace("Data was replicated successfully.");
        return true;
    }
}