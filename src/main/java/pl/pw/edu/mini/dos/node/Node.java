package pl.pw.edu.mini.dos.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Node extends UnicastRemoteObject
        implements MasterNodeInterface, NodeNodeInterface, Serializable {
    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(Node.class);

    private String data = "new";
    private NodeMasterInterface master;

    public Node(String masterHost, String masterPort, String myIp) throws RemoteException {
        System.setProperty("java.rmi.server.hostname", myIp);   // TODO: It's necessary?

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
        Node node = new Node(args[0], args[1], args[2]);

        Scanner scanner = new Scanner (System.in);
        System.out.println("*Enter 'q' to stop node or 'n' to generate new data.");
        while(scanner.hasNext()) {
            String text = scanner.next();
            if(text.equals("q")) {
                break;
            } else if(text.equals("n")) {
                node.setData(Double.toString(Math.random()));
                logger.info("New data generated: " + node.getData());
            }
        }

        node.stopNode();
        logger.info("Node stopped!");
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void stopNode() {
        master = null;
        System.exit(0); // Unfortunately, this is only way, to close RMI...
    }

    @Override
    public ExecuteSQLOnNodeResponse executeSQLOnNode
            (ExecuteSQLOnNodeRequest executeSQLOnNodeRequest)
            throws RemoteException {

        // TODO parsing query and get needed tables
        List<String> tables = new ArrayList<>(1);
        tables.add("#4"); // Table #4 is needed

        InsertMetadataResponse insertMetadataResponse
                = master.insertMetadata(new InsertMetadataRequest(tables));

        String result = this.data;

        HashMap map = new HashMap<String, String>();
        map.put("data", executeSQLOnNodeRequest.getSql());

        for (int i = 0; i < insertMetadataResponse.getNodes().size(); i++) {
            map.put("id", i);

            InsertDataRequest insertDataRequest
                    = new InsertDataRequest(new Record[] {new Record(map)}, tables);

            InsertDataResponse insertDataResponse
                    = insertMetadataResponse.getNodes().get(i).insertData(insertDataRequest);
        }

        logger.debug("I'm sending executeSQLOnNodeResponse..");
        return new ExecuteSQLOnNodeResponse(result, ErrorEnum.NO_ERROR);
    }

    @Override
    public CheckStatusResponse checkStatus(CheckStatusRequest checkStatusRequest) throws RemoteException {
        return null;
    }


    @Override
    public InsertDataResponse insertData(InsertDataRequest insertDataRequest)
            throws RemoteException {
        logger.debug("I'm insert {"
                + Helper.ArrayToString(insertDataRequest.getData())
                + "} to table " + insertDataRequest.getTable());
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
