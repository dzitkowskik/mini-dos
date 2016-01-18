package pl.pw.edu.mini.dos.communication.masternode;

import pl.pw.edu.mini.dos.communication.nodemaster.NodeMasterInterface;

import java.io.Serializable;

public class UpdateMasterRequest implements Serializable {
    private NodeMasterInterface masterInterface;

    public UpdateMasterRequest(NodeMasterInterface masterInterface) {
        this.masterInterface = masterInterface;
    }

    public NodeMasterInterface getMasterInterface() {
        return masterInterface;
    }
}
