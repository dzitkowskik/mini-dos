package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.masternode.MasterNodeInterface;

import java.io.Serializable;

public class RegisterRequest implements Serializable {
    public MasterNodeInterface node;

    public RegisterRequest(MasterNodeInterface node) {
        this.node = node;
    }
}
