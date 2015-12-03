package pl.pw.edu.mini.dos.master.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.util.List;

public class PingNodes implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PingNodes.class);
    private List<RegisteredNode> nodes;
    private long spanTime;

    public PingNodes(List<RegisteredNode> nodes, long spanTime) {
        this.nodes = nodes;
        this.spanTime = spanTime;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                logger.debug("Pinging " + nodes.size() + " nodes...");
                ErrorEnum ok;
                for(RegisteredNode node : nodes){
                    ok = node.checkStatus();
                    if(!ok.equals(ErrorEnum.NO_ERROR)){
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
