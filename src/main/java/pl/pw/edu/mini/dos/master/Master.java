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
import pl.pw.edu.mini.dos.master.rmi.RMIServer;
import pl.pw.edu.mini.dos.master.node.RegisteredNode;
import pl.pw.edu.mini.dos.master.task.TaskManager;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Master
        extends UnicastRemoteObject
        implements NodeMasterInterface, ClientMasterInterface, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Master.class);
    private static final Config config = Config.getConfig();
    private RMIServer server;

    private Thread pingThread;
    private NodeManager nodeManager;
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
        pingThread = new Thread(new PingNodes(nodeManager, spanTime));
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
            String text = scanner.next();
            if (text.equals("q")) {
                scanner.close();
                break;
            }
        }

        master.stopMaster();
        logger.info("Master stopped!");
    }

    public void stopMaster() {
        pingThread.interrupt();
        server.stopService(Services.MASTER, this);
        dbManager.close();
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
            return new InsertMetadataResponse(null, ErrorEnum.NOT_ENOUGH_NODES);
        }
        // Insert row metadata (RowId, TableId, NodesIds)
        List<Integer> nodesIds = new ArrayList<>(nodes.size());
        List<NodeNodeInterface> nodesInterfaces = new ArrayList<>(nodes.size());
        for (RegisteredNode node : nodes) {
            nodesIds.add(node.getID());
            nodesInterfaces.add((NodeNodeInterface) node.getInterface());
        }
        ErrorEnum ok = dbManager.insertRow(
                insertMetadataRequest.getTable(), nodesIds);
        if (ok.equals(ErrorEnum.TABLE_NOT_EXIST)) {
            // The table of the insert doesn't exist
            return new InsertMetadataResponse(null, ErrorEnum.TABLE_NOT_EXIST);
        }
        return new InsertMetadataResponse(nodesInterfaces, ErrorEnum.NO_ERROR);
    }

    @Override
    public SelectMetadataResponse selectMetadata(SelectMetadataRequest selectMetadataRequest)
            throws RemoteException {
        List<String> tables = selectMetadataRequest.getTables();
        logger.info("Get metadata select request for tables: " + Helper.collectionToString(tables));
        if (tables == null || tables.size() == 0) {
            return new SelectMetadataResponse(null, null, ErrorEnum.ANOTHER_ERROR);
        }
        List<String> createTableStatements = dbManager.getCreateTableStatements(tables);
        List<Integer> nodesIDs = dbManager.getNodesHaveTables(tables);
        if (createTableStatements == null || nodesIDs == null) {
            return new SelectMetadataResponse(null, null, ErrorEnum.TABLE_NOT_EXIST);
        }
        logger.info("Nodes which have the data: " + Helper.collectionToString(nodesIDs));
        List<NodeNodeInterface> nodesInterfaces = new ArrayList<>(nodesIDs.size());
        for (Integer nodeID : nodesIDs) {
            nodesInterfaces.add(nodeManager.<NodeNodeInterface>getNodeInterface(nodeID));
        }
        return new SelectMetadataResponse(nodesInterfaces,
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
        return null;
    }

    @Override
    public CreateMetadataResponse createMetadata(CreateMetadataRequest createMetadataRequest)
            throws RemoteException {
        return new CreateMetadataResponse(
                nodeManager.<NodeNodeInterface>getNodesInterfaces(),
                dbManager.insertTable(
                        createMetadataRequest.getTable(),
                        createMetadataRequest.getCreateStatement()));
    }

    @Override
    public ExecuteSQLResponse executeSQL(ExecuteSQLRequest executeSQLRequest) throws RemoteException {
        RegisteredNode node = nodeManager.selectCoordinatorNode();
        Long taskID = taskManager.newTask(node.getID());
        ExecuteSQLOnNodeResponse result = node.getInterface().executeSQLOnNode(
                new ExecuteSQLOnNodeRequest(taskID, executeSQLRequest.getSql()));
        return new ExecuteSQLResponse(result);
    }
}