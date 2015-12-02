package pl.pw.edu.mini.dos.communication.nodenode;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeNodeInterface extends Remote {
    /**
     * Execute insert query on another RegisteredNode.
     * @param insertDataRequest
     * Data for executing request.
     * @return
     * If executing query was successful.
     */
    InsertDataResponse insertData(InsertDataRequest insertDataRequest)
            throws RemoteException;
    /**
     * Execute select query on another RegisteredNode.
     * @param selectDataRequest
     * Data for executing request.
     * @return
     * If executing query was successful.
     */
    SelectDataResponse selectMetadata(SelectDataRequest selectDataRequest)
            throws RemoteException;
    /**
     * Execute update query on another RegisteredNode.
     * @param updateDataRequest
     * Data for executing request.
     * @return
     * If executing query was successful.
     */
    UpdateDataResponse updateMetadata(UpdateDataRequest updateDataRequest)
            throws RemoteException;
    /**
     * Execute delete query on another RegisteredNode.
     * @param deleteDataRequest
     * Data for executing request.
     * @return
     * If executing query was successful.
     */
    DeleteDataResponse deleteMetadata(DeleteDataRequest deleteDataRequest)
            throws RemoteException;
    /**
     * Manage tables on another RegisteredNode.
     * @param tableDataRequest
     * Data for executing request.
     * @return
     * If executing query was successful.
     */
    TableDataResponse tableMetadata(TableDataRequest tableDataRequest)
            throws RemoteException;
}
