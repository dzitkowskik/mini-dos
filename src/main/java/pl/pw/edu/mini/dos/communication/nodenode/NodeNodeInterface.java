package pl.pw.edu.mini.dos.communication.nodenode;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeNodeInterface extends Remote {
    /**
     * Execute insert query on another Node.
     * @param insertDataRequest
     * Data for executing request.
     * @return
     * If executing query was successful.
     */
    InsertDataResponse insertData(InsertDataRequest insertDataRequest);
    /**
     * Execute select query on another Node.
     * @param selectDataRequest
     * Data for executing request.
     * @return
     * If executing query was successful.
     */
    SelectDataResponse selectMetdata(SelectDataRequest selectDataRequest)
            throws RemoteException;
    /**
     * Execute update query on another Node.
     * @param updateDataRequest
     * Data for executing request.
     * @return
     * If executing query was successful.
     */
    UpdateDataResponse updateMetdata(UpdateDataRequest updateDataRequest)
            throws RemoteException;
    /**
     * Execute delete query on another Node.
     * @param deleteDataRequest
     * Data for executing request.
     * @return
     * If executing query was successful.
     */
    DeleteDataResponse deleteMetdata(DeleteDataRequest deleteDataRequest)
            throws RemoteException;
    /**
     * Manage tables on another Node.
     * @param tableDataRequest
     * Data for executing request.
     * @return
     * If executing query was successful.
     */
    TableDataResponse tableMetdata(TableDataRequest tableDataRequest)
            throws RemoteException;
}
