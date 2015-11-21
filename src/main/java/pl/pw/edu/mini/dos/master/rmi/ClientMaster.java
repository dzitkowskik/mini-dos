package pl.pw.edu.mini.dos.master.rmi;

import pl.pw.edu.mini.dos.communication.clientmaster.ClientMasterInterface;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLRequest;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLResponse;
import pl.pw.edu.mini.dos.master.Master;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientMaster extends UnicastRemoteObject
        implements ClientMasterInterface {
    private static final long serialVersionUID = 1L;
    Master master;

    protected ClientMaster(Master master) throws RemoteException {
        super();
        this.master = master;
    }

    @Override
    public ExecuteSQLResponse executeSQL(ExecuteSQLRequest executeSQLRequest) throws RemoteException {
        String query = executeSQLRequest.getSql();
        // TODO
        return new ExecuteSQLResponse("Your result :)");
    }
}
