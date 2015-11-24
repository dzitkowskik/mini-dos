package pl.pw.edu.mini.dos.master;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
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
import pl.pw.edu.mini.dos.master.rmi.RMIServer;
import pl.pw.edu.mini.dos.master.node.Node;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Master extends UnicastRemoteObject
        implements NodeMasterInterface, ClientMasterInterface, Serializable {
    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(Master.class);

    private RMIServer server;
    private final List<Node>  nodes;

    public Master() throws RemoteException {
        this("localhost", 1099);
    }

    public Master(String host, int port) throws RemoteException {
        nodes = new ArrayList<>();
        server = new RMIServer(host, port);
        server.startService(Services.MASTER, this);
        logger.info("Master listening at (" + host + ":" + port + ")");
    }

    /**
     * @param args = {"localhost", "1099"}
     */
    public static void main(String[] args) throws RemoteException {
        Master master;
        if(args.length == 2)
            master = new Master(args[0], Integer.valueOf(args[1]));
        else master = new Master();
        
        Scanner scanner = new Scanner (System.in);
        System.out.println("*Enter 'q' to stop master or 'd' to show the data of nodes:");
        while(scanner.hasNext()) {
            String text = scanner.next();
            if(text.equals("q")) {
                break;
            } else if(text.equals("d")){
                master.showNodesData();
            }
        }

        master.stopMaster();
        logger.info("Master stopped!");
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void showNodesData(){
        for (int i = 0; i < nodes.size(); i++) {
            try {
                System.out.println("Data from Node " + i + ": "
                        + nodes.get(i).getInterface().executeSQLOnNode(
                        new ExecuteSQLOnNodeRequest("SELECT * FROM *;")
                ).getResult());
            } catch (RemoteException e) {
                ErrorHandler.handleError(e, false);
            }
        }
    }
    public void stopMaster(){
        server.stopService(Services.MASTER, this);
    }

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) throws RemoteException {
        ErrorEnum ok;

        // Create node
        Node newNode  = new Node(registerRequest.getNode());

        // Check status (uncomment when it's implemented in node)
//        ok = newNode.checkStatus();
//        if(!ok.equals(ErrorEnum.NO_ERROR)){
//            return new RegisterResponse(ok);
//        }

        synchronized (nodes) {
            nodes.add(newNode);
        }
        logger.info("Node added.");

        return new RegisterResponse(ErrorEnum.NO_ERROR);
    }

    @Override
    public InsertMetadataResponse insertMetadata(InsertMetadataRequest insertMetadataRequest)
            throws RemoteException {
        List<NodeNodeInterface> nodes = new ArrayList<>(this.getNodes().size());
        // Insert in all nodes (example)
        for(Node n : this.getNodes()){
            nodes.add((NodeNodeInterface) n.getInterface());
        }

        return new InsertMetadataResponse(nodes, ErrorEnum.NO_ERROR);
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
        String query = executeSQLRequest.getSql();
        MasterNodeInterface node = nodes.get(selectNode()).getInterface();

        ExecuteSQLOnNodeResponse result = node.executeSQLOnNode(
                new ExecuteSQLOnNodeRequest(query));

        ExecuteSQLResponse response;
        if(result.getError().equals(ErrorEnum.NO_ERROR)){
            response = new ExecuteSQLResponse(result.getResult());
        } else {
            response = new ExecuteSQLResponse(result.getError().toString());
        }
        return response;
    }

    /**
     * Load balancer
     * @return node chosen to run the query
     */
    private synchronized int selectNode(){
        Random random = new Random();
        return random.nextInt(nodes.size());
    }
}