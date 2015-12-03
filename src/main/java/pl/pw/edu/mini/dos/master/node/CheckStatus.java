package pl.pw.edu.mini.dos.master.node;

import pl.pw.edu.mini.dos.communication.masternode.CheckStatusRequest;
import pl.pw.edu.mini.dos.communication.masternode.CheckStatusResponse;
import pl.pw.edu.mini.dos.communication.masternode.MasterNodeInterface;

import java.rmi.RemoteException;
import java.util.concurrent.Callable;

public class CheckStatus implements Callable<CheckStatusResponse> {
    MasterNodeInterface node;

    public CheckStatus(MasterNodeInterface node) {
        this.node = node;
    }

    @Override
    public CheckStatusResponse call() throws RemoteException {
        return node.checkStatus(new CheckStatusRequest());
    }
}
