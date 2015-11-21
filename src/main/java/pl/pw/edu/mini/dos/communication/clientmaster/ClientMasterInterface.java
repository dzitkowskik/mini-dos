package pl.pw.edu.mini.dos.communication.clientmaster;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientMasterInterface extends Remote {
    /**
     * Execute SQL query on Master as a Client.
     * @param executeSQLRequest
     * Data for executing request.
     * @return
     * Result of query.
     */
    ExecuteSQLResponse  executeSQL(ExecuteSQLRequest executeSQLRequest) throws RemoteException;
}
