package pl.pw.edu.mini.dos.master.node;

import pl.pw.edu.mini.dos.Config;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.masternode.CheckStatusResponse;
import pl.pw.edu.mini.dos.communication.masternode.MasterNodeInterface;

import java.util.concurrent.*;

public class RegisteredNode {
    private static final Config config = Config.getConfig();
    private Integer nodeID;
    private MasterNodeInterface node;
    StatusNode statusNode;
    private boolean needToUpdate;

    public RegisteredNode(MasterNodeInterface node) {
        this.node = node;
        this.statusNode = new StatusNode();
        this.needToUpdate = false;
    }

    public Integer getID() {
        return nodeID;
    }

    public void setID(Integer nodeID) {
        this.nodeID = nodeID;
    }

    public boolean isDown() {
        return statusNode.isDown();
    }

    public MasterNodeInterface getInterface() {
        return node;
    }

    public void setNeedToUpdate(boolean needToUpdate) {
        this.needToUpdate = needToUpdate;
    }

    public boolean isNeedToUpdate() {
        return needToUpdate;
    }

    /**
     * Checks if the node is up or down. If the node is up, it updates the status figures.
     *
     * @return true if the node is up, otherwise false
     */
    public ErrorEnum checkStatus() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<CheckStatusResponse> response = executor.submit(new CheckStatus(node));
        try {
            // Try to get status, if timeout expires -> node down
            CheckStatusResponse status = response.get(
                    Long.parseLong(config.getProperty("timeout")),
                    TimeUnit.MILLISECONDS);
            this.statusNode.setStatus(
                    status.getLoadAverage(),
                    status.getDbSize(),
                    status.getMemory());
        } catch (ExecutionException e) {
            this.statusNode.setDown();
            return ErrorEnum.REMOTE_EXCEPTION;
        } catch (TimeoutException e) {
            response.cancel(true);
            this.statusNode.setDown();
            return ErrorEnum.TIMEOUT_EXPIRED;
        } catch (InterruptedException e) {
            return ErrorEnum.ANOTHER_ERROR;
        } finally {
            executor.shutdownNow();
        }
        return ErrorEnum.NO_ERROR;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RegisteredNode node = (RegisteredNode) o;
        return nodeID.equals(node.nodeID);
    }

    @Override
    public int hashCode() {
        return nodeID.hashCode();
    }

    @Override
    public String toString() {
        return "#" + nodeID + ": " + statusNode;
    }
}


