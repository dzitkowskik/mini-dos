package pl.pw.edu.mini.dos.testclass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Helper;
import pl.pw.edu.mini.dos.master.Master;
import pl.pw.edu.mini.dos.master.node.NodeManager;
import pl.pw.edu.mini.dos.master.node.RegisteredNode;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: asd
 * Date: 1/8/16
 * Time: 8:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class FlexibleNodeManager extends NodeManager {
    private static final Logger logger = LoggerFactory.getLogger(FlexibleNodeManager.class);

    public FlexibleNodeManager(int replicationFactor) {
        super(replicationFactor);
    }

    public void setRegisteredNodes(List<RegisteredNode> nodes) {
        registeredNodes.clear();
        for (RegisteredNode node : nodes) {
            registeredNodes.put(node.getID(), node);
        }
    }

}