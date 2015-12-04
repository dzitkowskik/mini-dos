package pl.pw.edu.mini.dos.master;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Config;
import pl.pw.edu.mini.dos.communication.ErrorHandler;
import pl.pw.edu.mini.dos.communication.Services;
import pl.pw.edu.mini.dos.communication.clientmaster.ClientMasterInterface;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLRequest;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLResponse;
import pl.pw.edu.mini.dos.communication.masternode.ExecuteSQLOnNodeRequest;
import pl.pw.edu.mini.dos.communication.masternode.ExecuteSQLOnNodeResponse;
import pl.pw.edu.mini.dos.communication.masternode.MasterNodeInterface;
import pl.pw.edu.mini.dos.communication.nodemaster.*;
import pl.pw.edu.mini.dos.communication.nodenode.NodeNodeInterface;
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

    public Master() throws RemoteException {
        this("127.0.0.1", 1099);
    }

    public Master(String host, int port) throws RemoteException {
        // Get managers
        nodeManager = new NodeManager();
        taskManager = new TaskManager();

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
            } else if (text.equals("d")) {
                master.showNodesData();
            }
        }

        master.stopMaster();
        logger.info("Master stopped!");
    }

    public void stopMaster() {
        pingThread.interrupt();
        server.stopService(Services.MASTER, this);
    }

    public void showNodesData() {
        for (Map.Entry<Integer, RegisteredNode> node : nodeManager.getNodesMap().entrySet()) {
            try {
                System.out.println("Data from RegisteredNode " + node.getKey() + ": "
                        + node.getValue().getInterface().executeSQLOnNode(
                        new ExecuteSQLOnNodeRequest(taskManager.newTask(node.getKey()),
                                "SELECT * FROM *;")
                ).getResult());
            } catch (RemoteException e) {
                ErrorHandler.handleError(e, false);
            }
        }
    }

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) throws RemoteException {
        return new RegisterResponse(
                nodeManager.newNode(registerRequest.getNode()));
    }

    @Override
    public InsertMetadataResponse insertMetadata(InsertMetadataRequest insertMetadataRequest)
            throws RemoteException {
        List<NodeNodeInterface> nodes = new ArrayList<>(nodeManager.numNodes());
        // Insert in all registeredNodes (example)
        for (MasterNodeInterface n : nodeManager.getNodesInterfaces()) {
            nodes.add((NodeNodeInterface) n);
        }
        return new InsertMetadataResponse(nodes);
    }

    @Override
    public SelectMetadataResponse selectMetadata(SelectMetadataRequest selectMetadataRequest)
            throws RemoteException {
        return null;
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
    public TableMetadataResponse tableMetadata(TableMetadataRequest tableMetadataRequest)
            throws RemoteException {
        return null;
    }

    @Override
    public ExecuteSQLResponse executeSQL(ExecuteSQLRequest executeSQLRequest) throws RemoteException {
        // Select node
        Map.Entry<Integer, RegisteredNode> nodeEntry = nodeManager.selectNode();
        MasterNodeInterface nodeInterface = nodeEntry.getValue().getInterface();
        Integer nodeID = nodeEntry.getKey();

        Long taskID = taskManager.newTask(nodeID);
        ExecuteSQLOnNodeResponse result = nodeInterface.executeSQLOnNode(
                new ExecuteSQLOnNodeRequest(taskID, executeSQLRequest.getSql()));
        return new ExecuteSQLResponse(result);
    }
}