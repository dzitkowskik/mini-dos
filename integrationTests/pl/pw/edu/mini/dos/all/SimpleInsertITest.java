package pl.pw.edu.mini.dos.all;

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Config;
import pl.pw.edu.mini.dos.DockerStuff.DockerRunner;
import pl.pw.edu.mini.dos.DockerStuff.DockerThread;
import pl.pw.edu.mini.dos.Helper;
import pl.pw.edu.mini.dos.TestData;
import pl.pw.edu.mini.dos.TestsHelper;
import pl.pw.edu.mini.dos.client.Client;
import pl.pw.edu.mini.dos.master.Master;
import pl.pw.edu.mini.dos.master.MasterDecapsulation;
import pl.pw.edu.mini.dos.master.node.NodeManager;
import pl.pw.edu.mini.dos.master.node.PingNodes;
import pl.pw.edu.mini.dos.master.node.RegisteredNode;
import pl.pw.edu.mini.dos.node.Node;

import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static pl.pw.edu.mini.dos.TestsHelper.*;

/**
 * Created with IntelliJ IDEA.
 * User: asd
 * Date: 1/5/16
 * Time: 8:30 AM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleInsertITest {
    private static final Logger logger = LoggerFactory.getLogger(SimpleInsertITest.class);

    String configTestFilename1 = "testDbConfig.txt";

    int replicationFactor = 2;
    int nodesCount = 10;
    int dataCount = 3 * nodesCount;

    public void testBasicInsert_Master(String[] args) throws Exception {
        Master master = new Master(getMyIpFromParams(args),
                Integer.valueOf(getMasterPortFromParams(args)));

        // set my testLowBalancer
        TestNodeManager nodeManager = new TestNodeManager(replicationFactor);
        MasterDecapsulation.setNodeManager(master, nodeManager);
        MasterDecapsulation.getPingThread(master).interrupt();

        // refresh PingThread
        long spanTime = Long.parseLong(Config.getConfig().getProperty("spanPingingTime"));
        Thread pingThread = new Thread(new PingNodes(nodeManager, spanTime));
        pingThread.start();
        MasterDecapsulation.setPingThread(master, pingThread);

        // wait
        Scanner scanner = new Scanner(System.in);
        scanner.hasNext();

        master.stopMaster();
    }

    public void testBasicInsert_Client(String[] args) throws Exception {
        Client client = new Client(getMasterIpFromParams(args),
                getMasterPortFromParams(args), getMyIpFromParams(args));

        // load command
        TestData testData = TestData.loadConfigTestDbFile(configTestFilename1);
        logger.info("cmd=" + testData);
        logger.info("cmd.len=" + testData.insertTableCommands.size());

        // send command to Master
        for (String cmd : testData.createTableCommands) {
            client.executeSQL(cmd);
        }
        for (int i = 0; i < dataCount; i++) {
            client.executeSQL(testData.insertTableCommands.get(i));
        }

        client.stopClient();
    }

    public void testBasicInsert_Node(String[] args) throws Exception {
        Node node = new Node(getMasterIpFromParams(args),
                getMasterPortFromParams(args), getMyIpFromParams(args));

        TestData testData = TestData.loadConfigTestDbFile(getMyParams(args)[0]);
        //int dataCount = testData.insertTableCommands.size();

        // wait for any data (table not exist now)
        TestsHelper.Sleep(7);

        // wait for specific number of row in table
        String tableName = testData.getTableNames()[0];
        while (getNodeDbRowsCount(node, tableName) < dataCount * replicationFactor / nodesCount) {
            TestsHelper.Sleep(1);
        }

        // check current data from database with data created by cmd from cmd file
        checkDatabasesData(node, tableName, testData);

        node.stopNode();
    }

    @Test
    public void testBasicInsertSetUp() throws Exception {
        DockerRunner dockerRunner = DockerRunner.getInstance();

        // run Master, with testLowBalancer
        dockerRunner.runTestInDocker
                (SimpleInsertITest.class, "testBasicInsert_Master", null, "Master", true);
        TestsHelper.Sleep(5);

        // run Nodes
        DockerThread[] nodes = new DockerThread[nodesCount];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = dockerRunner.runTestInDocker(SimpleInsertITest.class,
                    "testBasicInsert_Node", new String[]{configTestFilename1}, "Node #" + i);
        }

        Sleep(1);
        // run test on Client side
        DockerThread clientThread = dockerRunner.runTestInDocker
                (SimpleInsertITest.class, "testBasicInsert_Client", null, "Client");

        // wait for Client and Nodes end
        clientThread.join();
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].join();
        }

        dockerRunner.stopThreads();

        // if closed correctly then weren't errors
        assertEquals(0, clientThread.exitVal);
        for (int i = 0; i < nodes.length; i++) {
            assertEquals(0, nodes[i].exitVal);
        }
    }

    @After
    public void tearDown() throws Exception {
        DockerRunner.getInstance().stopThreads();
    }

    // --------------------------------------------------------------------------
    // Classes, which was changed for tests
    // --------------------------------------------------------------------------

    class TestNodeManager extends NodeManager {
        int j = 0;

        public TestNodeManager(int replicationFactor) {
            super(replicationFactor);
        }

        @Override
        protected List<RegisteredNode> shuffle(List<RegisteredNode> nodes) {
            j++;

            logger.info(Helper.collectionToString(nodes));
            for (int i = 0; i < j; i++) {
                RegisteredNode tmp = nodes.remove(0);
                nodes.add(tmp);
            }
            logger.info(Helper.collectionToString(nodes));
            return nodes;
        }

    }

}
