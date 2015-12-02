package pl.pw.edu.mini.dos.communication.masternode;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MasterNodeInterface extends Remote {
    /**
     * Execute SQL query on RegisteredNode as a Master.
     *
     * @param executeSQLOnNodeRequest Data for executing query.
     * @return Result of query.
     */
    ExecuteSQLOnNodeResponse executeSQLOnNode(ExecuteSQLOnNodeRequest executeSQLOnNodeRequest)
            throws RemoteException;

    /**
     * Send request for status on RegisteredNode. Mainly check is alive.
     *
     * @param checkStatusRequest Now this is empty class.
     * @return Status of RegisteredNode.
     */
    CheckStatusResponse checkStatus(CheckStatusRequest checkStatusRequest)
            throws RemoteException;
}
