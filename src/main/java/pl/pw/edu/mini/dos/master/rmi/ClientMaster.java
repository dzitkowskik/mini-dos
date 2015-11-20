package pl.pw.edu.mini.dos.master.rmi;

import pl.pw.edu.mini.dos.communication.clientmaster.ClientMasterInterface;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLRequest;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLResponse;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientMaster extends UnicastRemoteObject
        implements ClientMasterInterface {
    private static final long serialVersionUID = 1L;

    protected ClientMaster() throws RemoteException {
        super();
        // TODO Auto-generated constructor stub
    }

    @Override
    public ExecuteSQLResponse executeSQL(ExecuteSQLRequest executeSQLRequest) throws RemoteException {
        // TODO
        return new ExecuteSQLResponse("Your result :)");
    }
}
