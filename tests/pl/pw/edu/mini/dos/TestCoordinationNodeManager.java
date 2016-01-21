package pl.pw.edu.mini.dos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.master.node.RegisteredNode;
import pl.pw.edu.mini.dos.testclass.TestNodeManager;

import java.util.ArrayList;

/**
 * Created by asd on 1/19/16.
 */
public class TestCoordinationNodeManager extends TestNodeManager {
    private static final Logger logger = LoggerFactory.getLogger(TestCoordinationNodeManager.class);

    public TestCoordinationNodeManager(int replicationFactor) {
        super(replicationFactor);
    }

    int selectCoordinatorNodeIndex = 0;

    @Override
    public synchronized RegisteredNode selectCoordinatorNode() {
        logger.trace("selectCoordinatorNodeIndex=" + selectCoordinatorNodeIndex);
        logger.trace("registeredNodes.size()="+registeredNodes.size());
        logger.trace(Helper.collectionToString(new ArrayList<>(registeredNodes.keySet())));
        if (registeredNodes.get(
                (new ArrayList<>(registeredNodes.keySet()))
                        .get(selectCoordinatorNodeIndex)).getInterface() == null)
            logger.error("interface is null!");
        return registeredNodes.get(
                (new ArrayList<>(registeredNodes.keySet()))
                        .get(selectCoordinatorNodeIndex));
    }

    public synchronized void setCoordinatorNodeIndex(int index) {
        selectCoordinatorNodeIndex = index;
    }
}
