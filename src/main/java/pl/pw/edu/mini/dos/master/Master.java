package pl.pw.edu.mini.dos.master;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.master.node.Node;

import java.util.HashMap;
import java.util.Map;

public class Master {
    private static final Logger logger = LoggerFactory.getLogger(Master.class);

    private Map<String, Node> nodes;

    public Master() {
        nodes = new HashMap<String, Node>();
    }

    /**
     * Register  a new node.
     * @param host node's IP
     * @return ErrorEnum
     */
    public ErrorEnum addNode(String host){
        Node newNode = new Node(host);
        // TODO check status
        nodes.put(host, new Node(host));
        return ErrorEnum.SUCCESS;
    }

    public static void main(String[] args) {
        logger.info("Hello world Mr. " + args[0] + "!");
    }
}
