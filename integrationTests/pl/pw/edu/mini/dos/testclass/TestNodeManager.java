package pl.pw.edu.mini.dos.testclass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Helper;
import pl.pw.edu.mini.dos.master.node.NodeManager;
import pl.pw.edu.mini.dos.master.node.RegisteredNode;
import pl.pw.edu.mini.dos.master.Master;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: asd
 * Date: 1/8/16
 * Time: 8:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestNodeManager extends NodeManager {
    private static final Logger logger = LoggerFactory.getLogger(TestNodeManager.class);

    int j = 0;
    public Master master;

    public TestNodeManager(int replicationFactor) {
        super(replicationFactor);
    }

    @Override
    protected List<RegisteredNode> shuffle(List<RegisteredNode> nodes) {
        for (int i = 0; i < j; i++) {
            RegisteredNode tmp = nodes.remove(0);
            nodes.add(tmp);
        }
        logger.info(Helper.collectionToString(nodes));

        j++;
        return nodes;
    }

    public void setRegisteredNodes(List<RegisteredNode> nodes) {
        registeredNodes.clear();
        for (RegisteredNode node : nodes) {
            registeredNodes.put(node.getID(), node);
        }
    }

}