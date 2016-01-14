package pl.pw.edu.mini.dos.all;

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.DockerStuff.DockerRunner;
import pl.pw.edu.mini.dos.DockerStuff.DockerThread;
import pl.pw.edu.mini.dos.TestData;
import pl.pw.edu.mini.dos.client.Client;
import pl.pw.edu.mini.dos.master.Master;
import pl.pw.edu.mini.dos.node.Node;

import java.sql.SQLException;
import java.util.Random;
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
public class AdvancedSelectITest {
    private static final Logger logger = LoggerFactory.getLogger(AdvancedSelectITest.class);

    String configTestFilename1 = "testDbConfig.txt";

    // settings
    int whereCommandCount = 300;
    int replicationFactor = 2;
    int nodesCount = 10;
    int dataCount = 10 * nodesCount;

    public void testAdvancedSelect_Master(String[] args) throws Exception {
        Master master = new Master(getMyIpFromParams(args),
                Integer.valueOf(getMasterPortFromParams(args)));

        // just wait
        Scanner scanner = new Scanner(System.in);
        scanner.hasNext();

        master.stopMaster();
    }

    public void testAdvancedSelect_Client(String[] args) throws Exception {
        Client client = new Client(getMasterIpFromParams(args),
                getMasterPortFromParams(args), getMyIpFromParams(args));

        // load command
        TestData testData = TestData.loadConfigTestDbFile(configTestFilename1);
        TestDbManager testDb = new TestDbManager();
        logger.info("cmd=" + testData);
        logger.info("cmd.len=" + testData.insertTableCommands.size());

        // send cmd to Master and run cmd on local test database
        for (String cmd : testData.createTableCommands) {
            runQuery(client, testDb, cmd);
        }
        for (int i = 0; i < dataCount; i++) {
            runQuery(client, testDb, testData.insertTableCommands.get(i));
        }

        // check all data
        String sqlGetAll = "SELECT * FROM " + testData.getTableNames()[0];
        String result = client.executeSQL(sqlGetAll);
        logger.info(result);

        checkDataCorrectness(result, testData.getTableNames()[0], testData, dataCount);
        checkQuery(client, testDb, sqlGetAll);

        // test select with where randomly
        checkWhere(client, testDb, testData, true);

        client.stopClient();
        logger.info("Client end");
    }

    private void checkWhere(Client client, TestDbManager testDb, TestData testData,
                            boolean testGroupBy)
            throws SQLException {
        logger.info("============================== Where tests ==============================");

        long seed = 345;
        Random rand = new Random(seed);
        String sql = "";
        int tableCount = testData.getTableNames().length;

        for (int i = 0; i < whereCommandCount; i++) {

            // get data
            String tableName = testData.getTableNames()[rand.nextInt(tableCount)];
            String[] colNames = testData.getColumnsNames(tableName);
            sql = String.format("SELECT * FROM %s WHERE", tableName);
            int whereCount = rand.nextInt(3);

            // build query
            int colIndex = rand.nextInt(colNames.length);
            sql += String.format(" %s = \"%s\"", colNames[colIndex],
                    testData.getRandomValueFromColumn(colIndex, dataCount));

            for (int w = 0; w < whereCount; w++) {
                colIndex = rand.nextInt(colNames.length);
                sql += (rand.nextBoolean() ? " AND" : " OR");
                sql += String.format(" %s = \"%s\"", colNames[colIndex],
                        testData.getRandomValueFromColumn(colIndex, dataCount));
            }

            // execute query
            String[] tmp = checkQuery(client, testDb, sql);
            /*String result = tmp[1];

            // there are more than 1 line in result  & testGroupBy
            if (result.split(System.lineSeparator()).length > 1 && testGroupBy) {
                checkGroupBy(client, testDb, testData, sql, tableName);
            }*/
        }
        logger.info("============================= Where tests end =============================");
    }

    /*private void checkGroupBy(Client client, TestDbManager testDb, TestData testData,
                              String sql, String tableName) throws SQLException {
        String[] colNames = testData.getColumnsNames(tableName);
        String sqlBase = sql + " GROUP BY ";
        for (String col : colNames) {
            checkQuery(client, testDb, sqlBase + col);
        }
    }*/


    public void testAdvancedSelect_Node(String[] args) throws Exception {
        Node node = null;
        try {
            node = new Node(getMasterIpFromParams(args),
                    getMasterPortFromParams(args), getMyIpFromParams(args));

            // wait
            Scanner scanner = new Scanner(System.in);
            scanner.hasNext();
        } finally {
            if (node != null)
                node.stopNode();
        }

        logger.info("Node end");
    }

    public void testAdvancedSelectSetUp() throws Exception {
        DockerRunner dockerRunner = DockerRunner.getInstance();

        // run Master
        dockerRunner.runMasterInDocker("Master");
        Sleep(5);

        // run Nodes
        DockerThread[] nodes = new DockerThread[nodesCount];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = dockerRunner.runNodeInDocker("Node #" + i);
            Sleep(0, 500);
        }

        Sleep(1);
        // run test on Client side
        DockerThread clientThread = dockerRunner.runTestInDocker
                (AdvancedSelectITest.class, "testAdvancedSelect_Client", null, "Client");

        // wait for Client and Nodes end
        clientThread.join();

        dockerRunner.stopThreads();

        // if closed correctly then weren't errors
        assertEquals(0, clientThread.exitVal);
    }


    @Test
    public void testAdvancedSelectV1() throws Exception {
        whereCommandCount = 10;
        replicationFactor = 2;
        nodesCount = 4;
        dataCount = 2 * nodesCount;

        testAdvancedSelectSetUp();
    }

    @Test
    public void testAdvancedSelectV2() throws Exception {
        whereCommandCount = 0;
        replicationFactor = 5;
        nodesCount = 10;
        dataCount = 100 * nodesCount;

        testAdvancedSelectSetUp();
    }

    @Test
    public void testAdvancedSelectV3() throws Exception {
        whereCommandCount = 100;
        replicationFactor = 2;
        nodesCount = 10;
        dataCount = 10 * nodesCount;

        testAdvancedSelectSetUp();
    }

    @After
    public void tearDown() throws Exception {
        DockerRunner.getInstance().stopThreads();
    }

}
