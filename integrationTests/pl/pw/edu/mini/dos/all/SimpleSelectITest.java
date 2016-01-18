package pl.pw.edu.mini.dos.all;

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.*;
import pl.pw.edu.mini.dos.DockerStuff.DockerRunner;
import pl.pw.edu.mini.dos.DockerStuff.DockerThread;
import pl.pw.edu.mini.dos.Utils.SendDataHelper;
import pl.pw.edu.mini.dos.Utils.TestDbManager;
import pl.pw.edu.mini.dos.Utils.TestsHelper;
import pl.pw.edu.mini.dos.client.Client;
import pl.pw.edu.mini.dos.master.Master;
import pl.pw.edu.mini.dos.master.MasterDecapsulation;
import pl.pw.edu.mini.dos.master.node.PingNodes;
import pl.pw.edu.mini.dos.node.Node;
import pl.pw.edu.mini.dos.testclass.TestNodeManager;

import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static pl.pw.edu.mini.dos.Utils.TestsHelper.*;

/**
 * Created with IntelliJ IDEA.
 * User: asd
 * Date: 1/5/16
 * Time: 8:30 AM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleSelectITest {
    private static final Logger logger = LoggerFactory.getLogger(SimpleSelectITest.class);

    String configTestFilename1 = "testDbConfig.txt";

    int replicationFactor = 2;
    int nodesCount = 10;
    Integer[] dataCounts = new Integer[]{3 * nodesCount, 3 * nodesCount};

    int oneCmdTime = 2; // seconds
    int nodeWaitingCount = 10;
    int nodeWaitingTime = Math.max((nodesCount - replicationFactor)
            * (oneCmdTime + 1) / (nodeWaitingCount - 1), 1);

    public void testBasicSelect_Master(String[] args) throws Exception {
        Master master = new Master(getMyIpFromParams(args),
                Integer.valueOf(getMasterPortFromParams(args)));

        logger.info("Mocking Master...");
        // set my testLowBalancer
        TestNodeManager nodeManager = new TestNodeManager(replicationFactor);
        MasterDecapsulation.setNodeManager(master, nodeManager);
        MasterDecapsulation.getPingThread(master).interrupt();

        // refresh PingThread
        long spanTime = Long.parseLong(Config.getConfig().getProperty("spanPingingTime"));
        Thread pingThread = new Thread(new PingNodes(master, nodeManager, spanTime));
        pingThread.start();
        MasterDecapsulation.setPingThread(master, pingThread);
        logger.info("Master mocked.");

        // wait
        logger.info("Master is waiting for request...");
        Scanner scanner = new Scanner(System.in);
        scanner.hasNext();

        master.stopMaster();
    }

    public void testBasicSelect_Client(String[] args) throws Exception {
        Client client = new Client(getMasterIpFromParams(args),
                getMasterPortFromParams(args), getMyIpFromParams(args));

        // load command
        TestData testData = TestData.loadConfigTestDbFile(configTestFilename1);
        TestDbManager testDb = new TestDbManager();
        SendDataHelper sendHelper = new SendDataHelper(testData, client, testDb);

        logger.trace("cmd=" + testData);
        logger.trace("cmd.len=" + testData.insertTableCommands.size());

        // send command to Master
        sendHelper.sendCreateQueries();
        sendHelper.sendInsertQueriesForAllTables(dataCounts);

        logger.info("Checking data...");
        checkDataCorrectnessForAllTable(client, testDb, testData, dataCounts);

        client.stopClient();
        logger.trace("Client end");
    }

    public void testBasicSelect_Node(String[] args) throws Exception {
        Node node = null;
        try {
            node = new Node(getMasterIpFromParams(args),
                    getMasterPortFromParams(args), getMyIpFromParams(args));
            logger.info("Node is waiting for request...");

            TestData testData = TestData.loadConfigTestDbFile(getMyParams(args)[0]);

            // now table not exists, so wait
            Sleep(15);

            String[] tableNames = testData.getTableNames();
            // wait for coming data
            for (int i = 0; i < tableNames.length; i++) {
                String tableName = tableNames[i];
                int oldSize = -1;
                int newSize = getNodeDbRowsCount(node, tableName);
                int count = 0;

                while (count < nodeWaitingCount) {
                    TestsHelper.Sleep(nodeWaitingTime);

                    if (oldSize < newSize)
                        count = 0;
                    else
                        count++;

                    oldSize = newSize;
                    newSize = getNodeDbRowsCount(node, tableName);
                    logger.trace("tableName=" + tableName + " count=" + count + " oldSize="
                            + oldSize + " newSize=" + newSize);
                }
            }

            // check correctness and integrity of data
            logger.info("Checking data...");
            int nodeId = getNodeIdFromIps(getMasterIpFromParams(args), getMyIpFromParams(args));
            logger.trace(String.valueOf(nodeId));
            TestNodeManager nodeManager = new TestNodeManager(replicationFactor);

            for (int i = 0; i < tableNames.length; i++) {
                String tableName = tableNames[i];
                List<Object[]> dataFromNode = getDataFromNodeDb(node, tableName);
                logger.trace("dataFromNode=" + dataFromNode);
                for (Object[] row : dataFromNode) {
                    logger.trace(Helper.arrayToString(row));
                }
                List<Integer> indexes = getDataIndexesPerNode(
                        nodeId, nodesCount, nodeManager, dataCounts[i]);
                logger.trace(Helper.collectionToString(indexes));
                checkDataCorrectnessOnNode(dataFromNode, tableName, testData);
            }
            logger.info("Data is OK.");

        } finally {
            if (node != null)
                node.stopNode();
        }

        logger.trace("Node end");
    }

    @Test
    public void testBasicSelectSetUp() throws Exception {
        DockerRunner dockerRunner = DockerRunner.getInstance();

        // run Master, with testLowBalancer
        dockerRunner.runTestInDocker
                (SimpleSelectITest.class, "testBasicSelect_Master", null, "Master", true);
        Sleep(5);

        // run Nodes
        DockerThread[] nodes = new DockerThread[nodesCount];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = dockerRunner.runTestInDocker(SimpleSelectITest.class,
                    "testBasicSelect_Node", new String[]{configTestFilename1}, "Node #" + i);
            Sleep(0, 500);
        }

        Sleep(1);
        // run test on Client side
        DockerThread clientThread = dockerRunner.runTestInDocker
                (SimpleSelectITest.class, "testBasicSelect_Client", null, "Client");

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

}
