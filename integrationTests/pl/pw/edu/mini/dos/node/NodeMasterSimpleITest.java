package pl.pw.edu.mini.dos.node;

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.DockerStuff.DockerRunner;
import pl.pw.edu.mini.dos.DockerStuff.DockerThread;
import pl.pw.edu.mini.dos.TestsHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static pl.pw.edu.mini.dos.TestsHelper.*;

/**
 * Created with IntelliJ IDEA.
 * User: asd
 * Date: 12/17/15
 * Time: 12:13 AM
 * To change this template use File | Settings | File Templates.
 */
public class NodeMasterSimpleITest {
    private static final Logger logger = LoggerFactory.getLogger(NodeMasterSimpleITest.class);

    public void testBasicCommunication_Node(String[] args) throws Exception {
        logger.info("Node started");
        Node node = new Node(getMasterIpFromParams(args),
                getMasterPortFromParams(args), getMyIpFromParams(args));

        assertNotNull(node.master);
        node.stopNode();
        logger.info("Node stopped.");
    }

    @Test
    public void testBasicRegisterSetUp() throws Exception {
        DockerRunner dockerRunner = DockerRunner.getInstance();
        dockerRunner.runMasterInDocker("Master");
        TestsHelper.Sleep(5);
        DockerThread thread = dockerRunner.runTestInDocker
                (NodeMasterSimpleITest.class, "testBasicCommunication_Node", null, "Node");

        thread.join();
        assertEquals(0, thread.exitVal);
    }

    @After
    public void tearDown() throws Exception {
        DockerRunner.getInstance().stopThreads();
    }

}
