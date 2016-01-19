package pl.pw.edu.mini.dos.master.node;


import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: asd
 * Date: 1/6/16
 * Time: 12:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class NodeOnMasterDecapsulation {
    public static void getStatusNode(RegisteredNode registeredNode, StatusNode statusNode) {
        registeredNode.statusNode = statusNode;
    }

    public static Map<Integer, RegisteredNode> getRegisteredNodes(NodeManager nodeManager) {
        return nodeManager.registeredNodes;
    }

}
