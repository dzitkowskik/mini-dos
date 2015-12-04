package pl.pw.edu.mini.dos.master.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.masternode.MasterNodeInterface;

import java.util.*;

public class NodeManager {
    private static final Logger logger = LoggerFactory.getLogger(NodeManager.class);
    private final Map<Integer, RegisteredNode> registeredNodes;
    private Integer nextNodeID;

    public NodeManager() {
        this.registeredNodes = new HashMap<>();
        this.nextNodeID = 0;
    }

    public ErrorEnum newNode(MasterNodeInterface nodeInterface) {
        // Create node
        RegisteredNode newNode = new RegisteredNode(nodeInterface);

        // Check status
        ErrorEnum ok = newNode.checkStatus();
        if (!ok.equals(ErrorEnum.NO_ERROR)) {
            return ok;
        }

        // Register node
        Integer nodeId;
        synchronized (registeredNodes) {
            nodeId = nextNodeID++;
            registeredNodes.put(nodeId, newNode);
        }
        logger.info("Node registered. nNodes: " + registeredNodes.size());
        return ErrorEnum.NO_ERROR;
    }

    public List<RegisteredNode> getNodes() {
        return new ArrayList<>(registeredNodes.values());
    }

    public List<MasterNodeInterface> getNodesInterfaces() {
        List<MasterNodeInterface> interfaces = new ArrayList<>(registeredNodes.size());
        for (RegisteredNode node : registeredNodes.values()) {
            interfaces.add(node.getInterface());
        }
        return interfaces;
    }

    public Map<Integer, RegisteredNode> getNodesMap() {
        return registeredNodes;
    }

    public int numNodes() {
        return registeredNodes.size();
    }

    /**
     * Load balancer
     *
     * @return node chosen to run the query
     */
    public synchronized Map.Entry<Integer, RegisteredNode> selectNode() {
        Random random = new Random();
        int r = random.nextInt(registeredNodes.size());
        Integer nodeID = (new ArrayList<>(registeredNodes.keySet())).get(r);
        return new AbstractMap.SimpleEntry<>(
                nodeID, registeredNodes.get(nodeID));
    }
}
