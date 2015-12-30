package pl.pw.edu.mini.dos.master.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.masternode.KillNodeRequest;
import pl.pw.edu.mini.dos.communication.masternode.MasterNodeInterface;

import java.rmi.RemoteException;
import java.util.*;

/**
 * It manages all the tasks related with nodes.
 */
public class NodeManager {
    private static final Logger logger = LoggerFactory.getLogger(NodeManager.class);
    final Map<Integer, RegisteredNode> registeredNodes;
    private Integer nextNodeID;
    private int replicationFactor;

    public NodeManager(int replicationFactor) {
        this.registeredNodes = new HashMap<>();
        this.nextNodeID = 0;
        this.replicationFactor = replicationFactor;
    }

    /**
     * Register new node on master.
     *
     * @param nodeInterface rmi interface of the node
     * @return ErrorEnum
     */
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
            newNode.setID(nodeId);
            registeredNodes.put(nodeId, newNode);
        }
        logger.info("Node registered. nNodes: " + registeredNodes.size());
        return ErrorEnum.NO_ERROR;
    }

    public List<RegisteredNode> getNodes() {
        return new ArrayList<>(registeredNodes.values());
    }

    public <T> List<T> getNodesInterfaces() {
        List<T> interfaces = new ArrayList<>(registeredNodes.size());
        for (RegisteredNode node : registeredNodes.values()) {
            interfaces.add((T) node.getInterface());
        }
        return interfaces;
    }

    public <T> T getNodeInterface(Integer nodeID) {
        return (T) registeredNodes.get(nodeID).getInterface();
    }

    public Map<Integer, RegisteredNode> getNodesMap() {
        return registeredNodes;
    }

    public int numNodes() {
        return registeredNodes.size();
    }

    /**
     * Selects a node that will be responsible of manage a client task.
     * The strategy used is to select a random node.
     *
     * @return node chosen
     */
    public synchronized RegisteredNode selectCoordinatorNode() {
        Random random = new Random(System.nanoTime());
        RegisteredNode node;
        do {
            int r = random.nextInt(registeredNodes.size());
            node = registeredNodes.get((new ArrayList<>(registeredNodes.keySet())).get(r));
        } while (node.isDown());
        return node;
    }

    /**
     * Select a list of nodes where data must be inserted.
     * The strategy used is to select a random list of node.
     * The number of nodes is determined by the configured replication factor.
     *
     * @return list of interfaces of nodes chosen
     */
    public synchronized List<RegisteredNode> selectNodesInsert() {
        Random random = new Random(System.nanoTime());
        List<RegisteredNode> nodes = new ArrayList<>(this.getNodes());
        Collections.shuffle(nodes, random);
        List<RegisteredNode> selectedNodes = new ArrayList<>(replicationFactor);
        Iterator<RegisteredNode> it = nodes.iterator();
        do {
            RegisteredNode node = it.next();
            if (!node.isDown()) {
                selectedNodes.add(node);
            }
        } while (selectedNodes.size() < replicationFactor && it.hasNext());
        if (selectedNodes.size() == replicationFactor) {
            return selectedNodes;
        }
        return null; // Not enough nodes
    }

    /**
     * @return all nodes with their information
     */
    public String select() {
        String result = "";
        for (RegisteredNode node : registeredNodes.values()) {
            result += node.toString() + "\n";
        }
        return result;
    }

    /**
     * @param nodeID node id
     * @return information of given task
     */
    public String select(Integer nodeID) {
        return registeredNodes.get(nodeID).toString() + "\n";
    }

    /**
     * Kill all nodes
     *
     * @return result
     */
    public String kill() {
        for (RegisteredNode node : registeredNodes.values()) {
            try {
                node.getInterface().killNode(new KillNodeRequest());
            } catch (RemoteException e) {
                return "Error when killing node " + node.getID() + ": " + e.getMessage() + "\n";
            }
        }
        return "All nodes killed\n";
    }


    /**
     * Kill given node
     *
     * @param nodeID node to kill
     * @return result
     */
    public String kill(Integer nodeID) {
        RegisteredNode node = registeredNodes.get(nodeID);
        try {
            node.getInterface().killNode(new KillNodeRequest());
        } catch (RemoteException e) {
            return "Error when killing node " + node.getID() + ": " + e.getMessage() + "\n";
        }
        return "Node " + node.getID() + " killed\n";
    }
}
