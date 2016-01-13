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
    private static final Config config = Config.getConfig();
    private RMIServer server;

    private Thread pingThread;
    NodeManager nodeManager;
    private TaskManager taskManager;
    private DBmanager dbManager;

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
        logger.info("Master listening at (" + host + ":" + port + ")");

        // Ping nodes periodically
        long spanTime = Long.parseLong(config.getProperty("spanPingingTime"));
        pingThread = new Thread(new PingNodes(this, nodeManager, spanTime));
        pingThread.start();
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
        System.out.println("*Enter 'q' to stop master or 'd' to show the data of nodes:");
        while (scanner.hasNext()) {
            String text = scanner.nextLine().toLowerCase();
            if (text.equals("q")) {
                scanner.close();
                break;
            }
            master.parseCommand(text);
        }

        master.stopMaster();
        logger.info("Master stopped!");
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
                        System.out.print(taskManager.select());
                    } else {
                        System.out.print(taskManager.select(Long.parseLong(matcher.group(2))));
                    }
                }
            } else {
                // Nodes
                if (matcher.group(1).equals("select")) {
                    if (matcher.group(2).equals("*")) {
                        System.out.print(nodeManager.select());
                    } else {
                        System.out.print(nodeManager.select(Integer.parseInt(matcher.group(2))));
                    }
                } else if (matcher.group(1).equals("kill")) {
                    if (matcher.group(2).equals("*")) {
                        System.out.print(nodeManager.kill());
                    } else {
                        System.out.print(nodeManager.kill(Integer.parseInt(matcher.group(2))));
                    }
                }
            }
        }
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
        logger.info("Get metadata select request for tables: " + Helper.collectionToString(tables));
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
            logger.info("Nodes which have table " + table + ": " + Helper.collectionToString(nodesIDs));
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
        return null;
    }

    @Override
    public DeleteMetadataResponse deleteMetadata(DeleteMetadataRequest deleteMetadataRequest)
            throws RemoteException {
        // Get nodes that have data from specified table
        List<Integer> nodesIDs = dbManager.getNodesHaveTable(deleteMetadataRequest.getTable());
        if (nodesIDs == null) {
            return new DeleteMetadataResponse(null, ErrorEnum.TABLE_NOT_EXIST);
        }
        logger.info("Nodes which have the data: " + Helper.collectionToString(nodesIDs));
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
        RegisteredNode node = nodeManager.selectCoordinatorNode();
        Long taskID = taskManager.newTask(node.getID());
        ExecuteSQLOnNodeResponse result = node.getInterface().executeSQLOnNode(
                new ExecuteSQLOnNodeRequest(taskID, executeSQLRequest.getSql().toUpperCase()));
        if (result.getError().equals(ErrorEnum.NO_ERROR)) {
            taskManager.setFinishedTask(taskID);
        } else {
            taskManager.setAbortedTask(taskID);
        }
        logger.info("Send response to client:" + result.getResult());
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
        logger.info("Unregistering node " + node.getID());
        // Get tables with data that node had
        Map<String, List<Long>> tablesRows = dbManager.getDataNodeHas(node);
        // Unregister node in master
        nodeManager.unregisterNode(node);
        dbManager.removeRecordsOfNode(node);
        // Send task to replicate data
        logger.debug("Replicate data that node had");
        Long taskID = taskManager.newTask(node.getID());
        // Get nodes which have the data
        logger.debug("Tables: " + Helper.collectionToString(tablesRows.keySet()));
        Map<String, List<NodeNodeInterface>> tableNodesInterfaces = new HashMap<>();
        for (String table : tablesRows.keySet()) {
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
        ReplicateDataResponse response = null;
        RegisteredNode newNode = nodeManager.selectNodesInsert().get(0);
        logger.debug("Replicate data in " + newNode.getID());
        try {
            response = newNode.getInterface().replicateData(
                    new ReplicateDataRequest(taskID, tableNodesInterfaces, tablesRows,
                            dbManager.getCreateTableStatements(new ArrayList<>(tablesRows.keySet()))));
        } catch (RemoteException e) {
            logger.error("Cannot replicate data: {}", e.getMessage());
        }
        if (response.getError().equals(ErrorEnum.NO_ERROR)) {
            logger.info("Data from node " + node.getID() + " was replicated successfully.");
        } else {
            logger.error("Error at replicating data: " + response.getError());
        }
    }

    /**
     * Reset (delete) all the data of a node.
     *
     * @param node node
     */
    @SuppressWarnings("ConstantConditions")
    public void resetDateNode(RegisteredNode node) {
        logger.info("Reset data of node " + node.getID());
        // Get all table names
        List<String> tables = dbManager.getTableNames();
        List<String> createTables = dbManager.getCreateTableStatements();
        ResetDataResponse response = null;
        try {
            response = node.getInterface().resetData(new ResetDataRequest(tables, createTables));
        } catch (RemoteException e) {
            logger.error("Cannot replicate data: {}", e.getMessage());
        }
        if (response.getError().equals(ErrorEnum.NO_ERROR)) {
            logger.info("Data from node " + node.getID() + " was reset successfully.");
        } else {
            logger.error("Error at reset data: " + response.getError());
        }
    }
}