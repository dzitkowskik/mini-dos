package pl.pw.edu.mini.dos.master.backup;

import pl.pw.edu.mini.dos.master.node.RegisteredNode;

import java.io.Serializable;
import java.util.Map;

public class NodeManagerBackup implements Serializable {
    protected Map<Integer, RegisteredNode> registeredNodes;
    private Map<RegisteredNode, Integer> downNodes;
    private Integer nextNodeID;

    public NodeManagerBackup(Map<Integer, RegisteredNode> registeredNodes,
                             Map<RegisteredNode, Integer> downNodes, Integer nextNodeID) {
        this.registeredNodes = registeredNodes;
        this.downNodes = downNodes;
        this.nextNodeID = nextNodeID;
    }

    public Map<Integer, RegisteredNode> getRegisteredNodes() {
        return registeredNodes;
    }

    public Map<RegisteredNode, Integer> getDownNodes() {
        return downNodes;
    }

    public Integer getNextNodeID() {
        return nextNodeID;
    }
}
