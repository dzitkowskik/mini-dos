package pl.pw.edu.mini.dos.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.DockerStuff.BashRunner;
import pl.pw.edu.mini.dos.DockerStuff.DockerRunner;
import pl.pw.edu.mini.dos.Helper;
import pl.pw.edu.mini.dos.TestData;
import pl.pw.edu.mini.dos.client.Client;
import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlRequest;
import pl.pw.edu.mini.dos.communication.nodenode.GetSqlResultResponse;
import pl.pw.edu.mini.dos.communication.nodenode.SerializableResultSet;
import pl.pw.edu.mini.dos.master.node.NodeOnMasterDecapsulation;
import pl.pw.edu.mini.dos.master.node.RegisteredNode;
import pl.pw.edu.mini.dos.master.node.StatusNode;
import pl.pw.edu.mini.dos.node.Node;
import pl.pw.edu.mini.dos.node.NodeDecapsulation;
import pl.pw.edu.mini.dos.testclass.TestNodeManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static pl.pw.edu.mini.dos.Helper.buildString;

public class TestsHelper {
    private static final Logger logger = LoggerFactory.getLogger(TestsHelper.class);
    static Random rand = new Random();

    public static void Sleep(int seconds) {
        for (int i = 0; i < seconds; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
    }

    public static void Sleep(int seconds, int milliseconds) {
        int delay = seconds * 1000 + milliseconds;

        for (int i = 0; i < delay / 100; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }

        try {
            Thread.sleep(delay % 100);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
    }

    public static String generateNetworkParams(DockerRunner dockerRunner) {
        String params = "";
        params += dockerRunner.getMasterIp() + " ";
        params += dockerRunner.getMasterPort() + " ";
        params += dockerRunner.getIP() + " ";
        return params;
    }

    public static String getMasterIpFromParams(String[] params) {
        return params[0];
    }

    public static String getMasterPortFromParams(String[] params) {
        return params[1];
    }

    public static String getMyIpFromParams(String[] params) {
        return params[2];
    }

    public static String[] getMyParams(String[] params) {
        return Arrays.copyOfRange(params, 3, params.length);
    }

    public static boolean checkCurrentIp(String[] args) {
        String ipExpected = args[0];
        String cmd[] = {"sh", "-c",
                "ifconfig"};

        String ip = "-";
        List<String> out;
        try {
            out = BashRunner.runCommandForResult(cmd);
            while (out.size() == 0) TestsHelper.Sleep(0, 100);

            logger.trace("out=" + out);
            Pattern pattern = Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");
            Matcher matcher = pattern.matcher(out.get(1));
            //logger.trace("out[1]:" + out.get(1));
            if (matcher.find()) {
                //logger.trace(matcher.group());
                ip = matcher.group();
            }
            if (!ipExpected.equals(ip)) {
                logger.error("TestIp fail! (" + ip + " != " + ipExpected + ")." +
                        " Check docker configuration.");
                return false;
            } else {
                logger.trace("TestIp passed. (" + ip + " == " + ipExpected + ")");
                return true;
            }
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage());
        }
        return false;
    }

    public static int getNodeDbRowsCount(Node node, String tableName) throws Exception {
        long taskId = 10000000 + rand.nextInt(100000);
        String rowsCountQuery = "SELECT COUNT(*) FROM " + tableName + ";";
        Callable<GetSqlResultResponse> queryRunner = NodeDecapsulation.getDBmanager(node)
                .newSQLJob(new ExecuteSqlRequest(taskId, rowsCountQuery, null));
        GetSqlResultResponse response = queryRunner.call();
        SerializableResultSet rs = response.getData();
        if (rs == null) {
            return 0;
        } else {
            return (int) rs.getData().get(0)[0];
        }
    }

    public static List<Object[]> getDataFromNodeDb(Node node, String tableName) throws Exception {
        long taskId = 10000000 + rand.nextInt(100000);
        String rowsCountQuery = "SELECT * FROM " + tableName + ";";
        Callable<GetSqlResultResponse> queryRunner = NodeDecapsulation.getDBmanager(node)
                .newSQLJob(new ExecuteSqlRequest(taskId, rowsCountQuery, null));
        GetSqlResultResponse response = queryRunner.call();
        if (response == null || response.getData() == null) {
            return null;
        } else {
            return response.getData().getData();
        }
    }

    public static List<Integer> getDataIndexesPerNode(int nodeId, int nodesCount, TestNodeManager nodeManager,
                                                      int dataCount) {
        List<Integer> indexes = new ArrayList<>();
        List<RegisteredNode> nodes = createFakeNodeList(nodesCount);

        nodeManager.setRegisteredNodes(nodes);

        for (int i = 0; i < dataCount; i++) {
            List<RegisteredNode> selectedNodes = nodeManager.selectNodesInsert();
            for (RegisteredNode node : selectedNodes) {
                if (node.getID() == nodeId) {
                    indexes.add(i);
                }
            }
        }

        return indexes;
    }

    // if part of data is on good Node database
    public static void checkDataIntegrity(
            List<Integer> indexes, List<Object[]> dataFromNode) {
        assertEquals(indexes.size(), dataFromNode.size());

        for (int i = 0; i < indexes.size(); i++) {
            int rowIdFromNode = (int) dataFromNode.get(i)[dataFromNode.get(i).length - 2];
            int testRowId = indexes.get(i);
            assertEquals("i=" + i + " indexes[i]=" + indexes.get(i), testRowId, rowIdFromNode);
        }
    }

    public static void checkDataCorrectnessForAllTable(
            Client client, TestDbManager testDb, TestData testData, Integer[] dataCounts)
            throws SQLException {
        String[] tableNames = testData.getTableNames();
        for (int i = 0; i < tableNames.length; i++) {
            logger.trace("table=" + tableNames[i] + "  dataCount=" + dataCounts[i]);
            String sqlGetAll = "SELECT * FROM " + testData.getTableNames()[i];
            String dataFromClient = checkQuery(client, testDb, sqlGetAll)[1];
            logger.trace(dataFromClient);
            checkDataCorrectness(dataFromClient, tableNames[i], testData, dataCounts[i]);
        }
    }

    public static void checkDataCorrectness(
            String dataFromClient, String tableName, TestData testData, int dataCount) {
        String[] rows = dataFromClient.split(System.lineSeparator());
        rows = Arrays.copyOfRange(rows, 2, rows.length);
        assertEquals(dataCount, rows.length);

        // remove bracket - []
        for (int i = 0; i < rows.length; i++) {
            rows[i] = rows[i].substring(1, rows[i].length() - 1);
        }

        for (int i = 0; i < dataCount; i++) {
            String rowFromClient = convertDataToCommand(rows[i].split(", "), tableName);
            logger.trace("rowFromClient = " + rowFromClient);
            int index = testData.insertTableCommands.get(tableName).indexOf(rowFromClient);
            assertTrue("Not found: " + rowFromClient, -1 < index); // exists
            assertTrue(index < dataCount); // in test's subset
        }
    }

    public static String[] checkQuery(
            Client client, TestDbManager testDb, String sql) throws SQLException {
        logger.trace("Checking query: " + sql);
        String orderBy = "";
        if (!sql.contains("ALTER")) orderBy = " ORDER BY 1, 2";
        SerializableResultSet rs = testDb.executeQuery(sql + orderBy);
        String resultExpected = buildString(rs.getData());

        logger.trace("Send: " + sql);
        String result = client.executeSQL(sql + orderBy);
        result = removeLastTwoColumns(result);
        logger.trace("Get:" + result);
        logger.trace("Expected:" + resultExpected);
        assertEquals("sql= " + sql, resultExpected, result);

        return new String[]{resultExpected, result};
    }

    public static String removeLastTwoColumns(String data) {
        String[] rows = data.split(System.lineSeparator());

        for (int i = 0; i < rows.length; i++) {
            if (rows[i].length() < 1) i++;
            int indexOfFirstComma = rows[i].lastIndexOf(",");
            if (indexOfFirstComma == -1) continue;
            int indexOfSecondComma = rows[i].lastIndexOf(",", indexOfFirstComma - 1);
            rows[i] = rows[i].substring(0, indexOfSecondComma) + "]";
        }
        return Helper.arrayToString(rows, System.lineSeparator());
    }

    public static void runQuery(
            Client client, TestDbManager testDb, String sql) throws SQLException {
        logger.trace("Checking query: " + sql);
        testDb.executeUpdate(sql);

        logger.trace("Send:" + sql);
        client.executeSQL(sql);
    }

    // doesn't test which data should be, only correctness data
    public static void checkDataCorrectnessOnNode(
            List<Object[]> dataFromNode, String tableName, TestData testData) {
        for (int i = 0; i < dataFromNode.size(); i++) {
            String rowFromNode = convertDataToCommand(dataFromNode.get(i), tableName, true);
            int rowId = (int) dataFromNode.get(i)[dataFromNode.get(i).length - 2];
            assertEquals(testData.insertTableCommands.get(tableName).get(rowId), rowFromNode);
        }
    }

    // dirty getting Node id from network configuration
    public static int getNodeIdFromIps(String masterIp, String nodeIp) {
        int masterLast = Integer.parseInt(masterIp.substring(masterIp.lastIndexOf(".") + 1));
        int nodeLast = Integer.parseInt(nodeIp.substring(nodeIp.lastIndexOf(".") + 1));

        return nodeLast - masterLast - 1;
    }

    public static List<RegisteredNode> createFakeNodeList(int count) {
        List<RegisteredNode> nodes = new ArrayList<>();

        StatusNode status = new StatusNode();
        status.setStatus(0, 0, 0);

        for (int i = 0; i < count; i++) {
            RegisteredNode node = new RegisteredNode(null);
            node.setID(i);
            NodeOnMasterDecapsulation.getStatusNode(node, status);
            nodes.add(node);
        }
        return nodes;
    }

    // temporary solution instead of running sql on local DB
    public static String convertDataToCommand(Object[] data, String tableName) {
        return convertDataToCommand(data, tableName, false);
    }

    public static String convertDataToCommand(Object[] data, String tableName, boolean ifFromNode) {
        if (data == null) return null;
        if (data.length == 0) return "";

        StringBuilder command = new StringBuilder("INSERT INTO " + tableName + " VALUES (\"");
        command.append(data[0].toString());

        // data form node: data.length - 2, because only client's data
        int len = (ifFromNode ? data.length - 2 : data.length);
        for (int i = 1; i < len; i++) {
            command.append("\",\"");
            command.append(data[i]);
        }
        command.append("\");");
        return command.toString();
    }

}