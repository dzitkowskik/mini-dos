package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.masternode.MasterNodeInterface;

import java.io.Serializable;

public class RegisterRequest implements Serializable {
    private MasterNodeInterface nodeInterface;

    public RegisterRequest(MasterNodeInterface nodeInterface) {
        this.nodeInterface = nodeInterface;
    }

    public MasterNodeInterface getNodeInterface() {
        return nodeInterface;
    }
}
