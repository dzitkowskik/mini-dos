package pl.pw.edu.mini.dos.master;

import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.ErrorHandler;
import pl.pw.edu.mini.dos.communication.Services;
import pl.pw.edu.mini.dos.communication.masternode.ExecuteSQLOnNodeRequest;
import pl.pw.edu.mini.dos.communication.masternode.MasterNodeInterface;
import pl.pw.edu.mini.dos.communication.nodemaster.*;
import pl.pw.edu.mini.dos.communication.nodenode.NodeNodeInterface;
import pl.pw.edu.mini.dos.master.rmi.RMIServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by asd on 11/19/15.
 */
public class Master extends UnicastRemoteObject implements NodeMasterInterface {
    RMIServer server;
    List<MasterNodeInterface> nodes = new ArrayList<>();

    public Master() throws RemoteException {

    }

    public Master(String host, int port) throws RemoteException {
        server = new RMIServer(host);
        server.StartService(Services.REGISTER, port, (NodeMasterInterface) this);

        System.out.println("Server listening at " + host + " (on " + port + ")");

        // demo

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if  (nodes.size() > 1) {
            int i = 0;
            try {
                System.out.println("==| Data from Node: "
                        + nodes.get(i).executeSQLOnNode(
                        new ExecuteSQLOnNodeRequest("some SQL query no " + i)
                ).result);
            } catch (RemoteException e) {
                ErrorHandler.handleError(e, false);
            }
        } else {
            System.out.println("I need 2 node to show demo!");
        }
        server.StopService(Services.REGISTER, this);
    }

    public static void main(String[] args) throws RemoteException {
        Master master = new Master(args[0], Integer.valueOf(args[1]));
        System.out.println("Server closing...");
    }

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) throws RemoteException {
        nodes.add(registerRequest.node);
        System.out.println("Node added.");

        return new RegisterResponse(ErrorEnum.NO_ERROR);
    }

    @Override
    public InsertMetadataResponse insertMetadata(InsertMetadataRequest insertMetadataRequest)
            throws RemoteException {
        InsertMetadataResponse insertMetadataResponse = new InsertMetadataResponse();
        insertMetadataResponse.nodes = new NodeNodeInterface[] {
                (NodeNodeInterface) nodes.get(0), (NodeNodeInterface) nodes.get(1)};
        insertMetadataResponse.error = ErrorEnum.NO_ERROR;
        System.out.println("I'm sending insertMetadataResponse..");
        return insertMetadataResponse;
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
}
