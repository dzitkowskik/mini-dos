package pl.pw.edu.mini.dos.master.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Config;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.master.Master;

/**
 * Checks the status of all registered nodes every spanTime milliseconds.
 */
public class PingNodes implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PingNodes.class);
    private static final Config config = Config.getConfig();
    private Master master;
    private NodeManager nodeManager;
    private long spanTime;
    private int maxPingRetryAttempts;

    public PingNodes(Master master, NodeManager nodeManager, long spanTime) {
        this.master = master;
        this.nodeManager = nodeManager;
        this.spanTime = spanTime;
        this.maxPingRetryAttempts = Integer.parseInt(config.getProperty("maxPingRetryAttempts"));
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                logger.debug("Pinging " + nodeManager.numNodes() + " nodes...");
                ErrorEnum ok;
                for (RegisteredNode node : nodeManager.getAllNodes()) {
                    ok = node.checkStatus();
                    if (!ok.equals(ErrorEnum.NO_ERROR)) {
                        // Node down
                        if (nodeManager.isNodeDown(node)) {
                            int numRetry = nodeManager.getNumRetrys(node) + 1;
                            logger.warn("Node " + node.getID() + " is down (" + numRetry + " retry): "
                                    + ok.toString());
                            if (numRetry > maxPingRetryAttempts) {
                                logger.warn("Max retry attempts exceeded -> unregister node " + node.getID());
                                // Limit exceeded -> unregister node and replicate its data
                                master.unregisterNode(node);
                            } else {
                                // Try again
                                nodeManager.setNodeDown(node, numRetry);
                            }
                        } else {
                            // First retry -> try again
                            logger.warn("Node " + node.getID() + " is down (1 retry): " + ok.toString());
                            nodeManager.setNodeDown(node, 1);
                        }
                    } else {
                        // Node up
                        if (nodeManager.isNodeDown(node)) {
                            // Node recovered
                            nodeManager.setNodeUp(node);
                            // If it was needed during down time -> need to update tables
                            if (node.isNeedToUpdate()) {
                                master.updateTablesNode(node);
                            }
                        }
                    }
                }
                Thread.sleep(spanTime);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
