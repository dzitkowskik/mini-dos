package pl.pw.edu.mini.dos.communication.nodemaster;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeMasterInterface extends Remote {
    /**
     * Register Node on Master.
     * @param registerRequest
     * Now is empty.
     * @return
     * If register was successful.
     */
    RegisterResponse register(RegisterRequest registerRequest)
            throws RemoteException;

    /**
     * Request for metadata to execute insert query.
     * @param insertMetadataRequest
     * Information which metadata needs.
     * @return
     * Received metadata.
     */
    InsertMetadataResponse insertMetdata(InsertMetadataRequest insertMetadataRequest)
            throws RemoteException;
    /**
     * Request for metadata to execute select query.
     * @param selectMetadataRequest
     * Information which metadata needs.
     * @return
     * Received metadata.
     */
    SelectMetadataResponse selectMetdata(SelectMetadataRequest selectMetadataRequest)
            throws RemoteException;

    /**
     * Request for metadata to execute update query.
     * @param updateMetadataRequest
     * Information which metadata needs.
     * @return
     * Received metadata.
     */
    UpdateMetadataResponse updateMetdata(UpdateMetadataRequest updateMetadataRequest)
            throws RemoteException;
    /**
     * Request for metadata to execute delete query.
     * @param deleteMetadataRequest
     * Information which metadata needs.
     * @return
     * Received metadata.
     */
    DeleteMetadataResponse deleteMetdata(DeleteMetadataRequest deleteMetadataRequest)
            throws RemoteException;
    /**
     * Request for metadata to manage tables.
     * @param tableMetadataRequest
     * Information which metadata needs.
     * @return
     * Received metadata.
     */
    TableMetadataResponse tableMetdata(TableMetadataRequest tableMetadataRequest)
            throws RemoteException;
}
