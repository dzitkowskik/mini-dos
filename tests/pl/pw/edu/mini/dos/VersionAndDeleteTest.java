package pl.pw.edu.mini.dos;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLRequest;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLResponse;
import pl.pw.edu.mini.dos.master.Master;
import pl.pw.edu.mini.dos.master.MasterDecapsulation;
import pl.pw.edu.mini.dos.master.node.PingNodes;
import pl.pw.edu.mini.dos.node.Node;
import pl.pw.edu.mini.dos.testclass.TestNodeManager;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static pl.pw.edu.mini.dos.Utils.TestsHelper.*;

/**
 * Created by asd on 1/19/16.
 */
public class VersionAndDeleteTest {
    private static final Logger logger = LoggerFactory.getLogger(VersionAndDeleteTest.class);

    String configTestFilename1 = "testDbConfig.txt";
    int replicationFactor = 2;
    private int oneCmdTime = 1;
    private int dataCount = 4;
    TestData testData;
    String tableName;

    @Test
    public void testVersionOnConcurrentUpdating() throws Exception {
        Master master = new Master();

        logger.info("Mocking Master...");
        // set my testLowBalancer
        TestCoordinationNodeManager nodeManager
                = new TestCoordinationNodeManager(replicationFactor);
        MasterDecapsulation.setNodeManager(master, nodeManager);
        MasterDecapsulation.getPingThread(master).interrupt();

        // refresh PingThread
        long spanTime = Long.parseLong(Config.getConfig().getProperty("spanPingingTime"));
        Thread pingThread = new Thread(new PingNodes(master, nodeManager, spanTime));
        pingThread.start();
        MasterDecapsulation.setPingThread(master, pingThread);

        logger.info("Master mocked.");

        LateNode node1 = new LateNode();
        Sleep(0, 500);
        Node node2 = new Node();
        Sleep(0, 500);
        Node node3 = new Node();
        Sleep(0, 500);

        testData = TestData.loadConfigTestDbFile(configTestFilename1);
        tableName = testData.getTableNames()[0];

        String sql = testData.createTableCommands.get(tableName);
        logger.info("Execute: " + sql);
        master.executeSQL(new ExecuteSQLRequest(sql));

        for (int i = 0; i < dataCount; i++) {
            sql = testData.insertTableCommands.get(tableName).get(i);
            logger.info(String.format("#%d Send to Master: %s", i, sql));

            master.executeSQL(new ExecuteSQLRequest(sql));
            Sleep(oneCmdTime);  // it's needed for prediction loadBalancer
        }

        // check
        checkNode(node1, 0, 3, new TestCoordinationNodeManager(replicationFactor));
        checkNode(node2, 1, 3, new TestCoordinationNodeManager(replicationFactor));
        checkNode(node3, 2, 3, new TestCoordinationNodeManager(replicationFactor));

        List<Object[]> dataFromNode1 = getDataFromNodeDb(node1, tableName);
        for (Object[] row : dataFromNode1) {
            logger.trace(Helper.arrayToString(row));
        }
        List<Object[]> dataFromNode2 = getDataFromNodeDb(node2, tableName);
        for (Object[] row : dataFromNode2) {
            logger.trace(Helper.arrayToString(row));
        }
        List<Object[]> dataFromNode3 = getDataFromNodeDb(node3, tableName);
        for (Object[] row : dataFromNode3) {
            logger.trace(Helper.arrayToString(row));
        }

        // select coordinate node
        ((TestCoordinationNodeManager) MasterDecapsulation.getNodeManager(master))
                .setCoordinatorNodeIndex(0);
        node1.SetVersionDelta(-100);

        sql = String.format("DELETE FROM %s WHERE row_id = 2", tableName);
        logger.trace("Send:" + sql);
        ExecuteSQLResponse response = master.executeSQL(new ExecuteSQLRequest(sql));
        assertEquals(ErrorEnum.NO_ERROR, response.getError());

        // for all
        CompareNodeData(dataFromNode1, node1, tableName);
        CompareNodeData(dataFromNode2, node2, tableName);
        CompareNodeData(dataFromNode3, node3, tableName);

        Sleep(1);
        master.stopMaster();
        node1.stopNode();
        node2.stopNode();
        node3.stopNode();
    }

    private void CompareNodeData(List<Object[]> dataFromNode,
                                 Node node, String tableName)
            throws Exception {
        List<Object[]> dataFromNode1_after = getDataFromNodeDb(node, tableName);
        assertEquals(dataFromNode.size(), dataFromNode1_after.size());
        logger.trace("dataFromNode_after:");
        for (int i = 0; i < dataFromNode.size(); i++) {
            logger.trace(Helper.arrayToString(dataFromNode1_after.get(i)));
            assertArrayEquals("i="+i, dataFromNode.get(i), dataFromNode1_after.get(i));
        }
    }

    private void checkNode(Node node, int nodeId,
                           int nodesCount, TestNodeManager nodeManager)
            throws Exception {
        List<Object[]> dataFromNode = getDataFromNodeDb(node, tableName);
        logger.trace("dataFromNode:");
        for (Object[] row : dataFromNode) {
            logger.trace(Helper.arrayToString(row));
        }
        List<Integer> indexes = getDataIndexesPerNode(
                nodeId, nodesCount, nodeManager, dataCount);
        logger.trace(Helper.collectionToString(indexes));
        checkDataCorrectnessOnNode(dataFromNode, tableName, testData);
        checkDataIntegrity(indexes, dataFromNode);
    }
}
