package pl.pw.edu.mini.dos.communication.nodemaster;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeMasterInterface extends Remote {
    /**
     * Register RegisteredNode on Master.
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
    InsertMetadataResponse insertMetadata(InsertMetadataRequest insertMetadataRequest)
            throws RemoteException;
    /**
     * Request for metadata to execute select query.
     * @param selectMetadataRequest
     * Information which metadata needs.
     * @return
     * Received metadata.
     */
    SelectMetadataResponse selectMetadata(SelectMetadataRequest selectMetadataRequest)
            throws RemoteException;

    /**
     * Request for metadata to execute update query.
     * @param updateMetadataRequest
     * Information which metadata needs.
     * @return
     * Received metadata.
     */
    UpdateMetadataResponse updateMetadata(UpdateMetadataRequest updateMetadataRequest)
            throws RemoteException;
    /**
     * Request for metadata to execute delete query.
     * @param deleteMetadataRequest
     * Information which metadata needs.
     * @return
     * Received metadata.
     */
    DeleteMetadataResponse deleteMetadata(DeleteMetadataRequest deleteMetadataRequest)
            throws RemoteException;
    /**
     * Request for metadata to manage tables.
     * @param tableMetadataRequest
     * Information which metadata needs.
     * @return
     * Received metadata.
     */
    TableMetadataResponse tableMetadata(TableMetadataRequest tableMetadataRequest)
            throws RemoteException;
}
