package pl.pw.edu.mini.dos.node;

import pl.pw.edu.mini.dos.Helper;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.ErrorHandler;
import pl.pw.edu.mini.dos.communication.Record;
import pl.pw.edu.mini.dos.communication.Services;
import pl.pw.edu.mini.dos.communication.masternode.*;
import pl.pw.edu.mini.dos.communication.nodemaster.*;
import pl.pw.edu.mini.dos.communication.nodenode.*;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

/**
 * Created by asd on 11/19/15.
 */
public class Node extends UnicastRemoteObject
        implements MasterNodeInterface, NodeNodeInterface, Serializable {
    RMIClient client;
    String data = "yui";
    NodeMasterInterface master;

    public Node() throws RemoteException {

    }

    public Node(String masterHost, String myIp) throws RemoteException {
        System.setProperty("java.rmi.server.hostname", myIp);   // TODO: It's necessary?

        client = new RMIClient(masterHost);
        try {
            master = (NodeMasterInterface) client.getService(Services.REGISTER);
            master.register(new RegisterRequest(this));
        } catch (RemoteException e) {
            ErrorHandler.handleError(e, true);
        }

        // demo

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // This is for check,
        //  if RMI just send serialized class or we have remote interface
        data = "asdasd";
        System.out.println("data changed");

        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        master = null;
        // Unfortunately, this is only way, to close RMI...
        System.exit(0);
    }

    public static void main(String[] args) throws URISyntaxException, RemoteException {
        Node node = new Node(args[0], args[1]);

        System.out.println("Node finished.");
    }

    @Override
    public ExecuteSQLOnNodeResponse executeSQLOnNode
            (ExecuteSQLOnNodeRequest executeSQLOnNodeRequest)
            throws RemoteException {
        ExecuteSQLOnNodeResponse executeSQLOnNodeResponse = new ExecuteSQLOnNodeResponse();

        // parsing query
        String table = "table #4";

        InsertMetadataResponse insertMetadataResponse
                = master.insertMetadata(new InsertMetadataRequest(table));
        executeSQLOnNodeResponse.result = "I executed: " + executeSQLOnNodeRequest.sql
                + " for (" + table + ", from Master ={"
                + "Array of nodes, len=" + insertMetadataResponse.nodes.length
                + "}) and result is: " + data;

        HashMap map = new HashMap<String, Object>();
        map.put("data", executeSQLOnNodeRequest.sql);

        for (int i = 0; i < insertMetadataResponse.nodes.length; i++) {
            map.put("id", i);

            InsertDataRequest insertDataRequest
                    = new InsertDataRequest(new Record[] {new Record(map)}, table);

            InsertDataResponse insertDataResponse
                    = insertMetadataResponse.nodes[i].insertData(insertDataRequest);
        }

        System.out.println("I'm sending executeSQLOnNodeResponse..");
        return executeSQLOnNodeResponse;
    }

    @Override
    public CheckStatusResponse checkStatus(CheckStatusRequest checkStatusRequest) throws RemoteException {
        return null;
    }


    @Override
    public InsertDataResponse insertData(InsertDataRequest insertDataRequest)
            throws RemoteException {
        System.out.println("I'm insert {"
                + Helper.ArrayToString(insertDataRequest.data)
                + "} to table " + insertDataRequest.table);
        return new InsertDataResponse(ErrorEnum.NO_ERROR);
    }

    @Override
    public SelectDataResponse selectMetadata(SelectDataRequest selectDataRequest)
            throws RemoteException {
        return null;
    }

    @Override
    public UpdateDataResponse updateMetadata(UpdateDataRequest updateDataRequest)
            throws RemoteException {
        return null;
    }

    @Override
    public DeleteDataResponse deleteMetadata(DeleteDataRequest deleteDataRequest)
            throws RemoteException {
        return null;
    }

    @Override
    public TableDataResponse tableMetadata(TableDataRequest tableDataRequest)
            throws RemoteException {
        return null;
    }
}
