package pl.pw.edu.mini.dos.master.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;

public class PingNodes implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PingNodes.class);
    private NodeManager nodeManager;
    private long spanTime;

    public PingNodes(NodeManager nodeManager, long spanTime) {
        this.nodeManager = nodeManager;
        this.spanTime = spanTime;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                logger.debug("Pinging " + nodeManager.numNodes() + " nodes...");
                ErrorEnum ok;
                for (RegisteredNode node : nodeManager.getNodes()) {
                    ok = node.checkStatus();
                    if (!ok.equals(ErrorEnum.NO_ERROR)) {
                        logger.warn(ok.toString());
                    }
                }
                Thread.sleep(spanTime);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
