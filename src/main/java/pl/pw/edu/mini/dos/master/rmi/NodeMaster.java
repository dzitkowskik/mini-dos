package pl.pw.edu.mini.dos.master.rmi;

import pl.pw.edu.mini.dos.communication.nodemaster.*;
import pl.pw.edu.mini.dos.master.Master;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class NodeMaster extends UnicastRemoteObject
        implements NodeMasterInterface {
    private static final long serialVersionUID = 1L;
    Master master;

    protected NodeMaster(Master master) throws RemoteException {
        super();
        this.master = master;
    }

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) throws RemoteException {
        return null;
    }

    @Override
    public InsertMetadataResponse insertMetadata(InsertMetadataRequest insertMetadataRequest) throws RemoteException {
        return null;
    }

    @Override
    public SelectMetadataResponse selectMetadata(SelectMetadataRequest selectMetadataRequest) throws RemoteException {
        return null;
    }

    @Override
    public UpdateMetadataResponse updateMetadata(UpdateMetadataRequest updateMetadataRequest) throws RemoteException {
        return null;
    }

    @Override
    public DeleteMetadataResponse deleteMetadata(DeleteMetadataRequest deleteMetadataRequest) throws RemoteException {
        return null;
    }

    @Override
    public TableMetadataResponse tableMetadata(TableMetadataRequest tableMetadataRequest) throws RemoteException {
        return null;
    }
}
