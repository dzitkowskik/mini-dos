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
    public InsertMetadataResponse insertMetdata(InsertMetadataRequest insertMetadataRequest) throws RemoteException {
        return null;
    }

    @Override
    public SelectMetadataResponse selectMetdata(SelectMetadataRequest selectMetadataRequest) throws RemoteException {
        return null;
    }

    @Override
    public UpdateMetadataResponse updateMetdata(UpdateMetadataRequest updateMetadataRequest) throws RemoteException {
        return null;
    }

    @Override
    public DeleteMetadataResponse deleteMetdata(DeleteMetadataRequest deleteMetadataRequest) throws RemoteException {
        return null;
    }

    @Override
    public TableMetadataResponse tableMetdata(TableMetadataRequest tableMetadataRequest) throws RemoteException {
        return null;
    }
}
