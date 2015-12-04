package pl.pw.edu.mini.dos.master.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Config;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.masternode.CheckStatusResponse;
import pl.pw.edu.mini.dos.communication.masternode.MasterNodeInterface;

import java.util.concurrent.*;


public class RegisteredNode {
    private static final Logger logger = LoggerFactory.getLogger(RegisteredNode.class);
    private static final Config config = Config.getConfig();

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
            return ErrorEnum.HOST_IS_UNAVAILABLE;
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
}


