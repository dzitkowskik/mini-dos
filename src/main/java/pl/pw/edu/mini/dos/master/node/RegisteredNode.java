package pl.pw.edu.mini.dos.master.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.masternode.CheckStatusRequest;
import pl.pw.edu.mini.dos.communication.masternode.CheckStatusResponse;
import pl.pw.edu.mini.dos.communication.masternode.MasterNodeInterface;

import java.rmi.RemoteException;

public class RegisteredNode {

    private static final Logger logger = LoggerFactory.getLogger(RegisteredNode.class);

    private MasterNodeInterface node;

    private StatusNode statusNode;

    public RegisteredNode(MasterNodeInterface node) {
        this.node = node;
        this.statusNode = new StatusNode();
    }

    public MasterNodeInterface getInterface() {
        return node;
    }

    public StatusNode getStatusNode() {
        return statusNode;
    }

    public void setStatusNode(StatusNode statusNode) {
        this.statusNode = statusNode;
    }

    /**
     * Checks if the node is up or down. If the node is up, it updates the status figures.
     * @return true if the node is up, otherwise false
     */
    public ErrorEnum checkStatus() {
        try {
            CheckStatusResponse status = node.checkStatus(new CheckStatusRequest());
            this.statusNode.setStatus(
                    status.getLoadAverage(),
                    status.getDbSize(),
                    status.getMemory());

        } catch (RemoteException e) {
            logger.warn("RegisteredNode is down");
            logger.warn("E: " + e.getMessage());
            this.statusNode.setDown();
            return ErrorEnum.HOST_IS_UNAVAILABLE;
        }
        return ErrorEnum.NO_ERROR;
    }
}


