package pl.pw.edu.mini.dos.all;

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.DockerStuff.DockerRunner;
import pl.pw.edu.mini.dos.DockerStuff.DockerThread;
import pl.pw.edu.mini.dos.TestData;
import pl.pw.edu.mini.dos.Utils.SendDataHelper;
import pl.pw.edu.mini.dos.Utils.Settings;
import pl.pw.edu.mini.dos.Utils.TestDbManager;
import pl.pw.edu.mini.dos.client.Client;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static pl.pw.edu.mini.dos.Utils.TestsHelper.*;

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

    public void testAdvancedSelect_Client(String[] args) throws Exception {
        Client client = new Client(getMasterIpFromParams(args),
                getMasterPortFromParams(args), getMyIpFromParams(args));

        ASSettings settings = new ASSettings().getSettingsFromParams(getMyParams(args));
        // load command
        TestData testData = TestData.loadConfigTestDbFile(configTestFilename1);
        TestDbManager testDb = new TestDbManager();
        SendDataHelper sendHelper = new SendDataHelper(testData, client, testDb);

        logger.trace("cmd=" + testData);
        logger.trace("cmd.len=" + testData.insertTableCommands.size());

        // send cmd to Master and run cmd on local test database
        sendHelper.sendCreateQueries();
        sendHelper.sendInsertQueriesForAllTables(settings.dataCounts);

        // check all data
        logger.info("Checking data...");
        checkDataCorrectnessForAllTable(client, testDb, testData, settings.dataCounts);
        logger.info("Data is OK.");

        // test select with where randomly
        logger.info("Checking select where queries...");
        checkWhere(client, testDb, testData, true, settings);
        logger.info("Test select where finished.");

        client.stopClient();
        logger.trace("Client end");
    }

    private void checkWhere(Client client, TestDbManager testDb, TestData testData,
                            boolean testGroupBy, ASSettings settings)
            throws SQLException {
        logger.trace("============================== Where tests ==============================");

        long seed = 345;
        Random rand = new Random(seed);
        String sql = "";
        int tableCount = testData.getTableNames().length;

        for (int i = 0; i < settings.whereCommandCount; i++) {

            // get data
            int tableIndex = rand.nextInt(tableCount);
            String tableName = testData.getTableNames()[tableIndex];
            String[] colNames = testData.getColumnsNames(tableName);
            sql = String.format("SELECT * FROM %s WHERE", tableName);
            int whereCount = rand.nextInt(3);

            // build query
            int colIndex = rand.nextInt(colNames.length);
            sql += String.format(" %s = \"%s\"", colNames[colIndex],
                    testData.getRandomValueFromColumn(
                            tableName, colIndex, settings.dataCounts[tableIndex]));

            for (int w = 0; w < whereCount; w++) {
                colIndex = rand.nextInt(colNames.length);
                sql += (rand.nextBoolean() ? " AND" : " OR");
                sql += String.format(" %s = \"%s\"", colNames[colIndex],
                        testData.getRandomValueFromColumn(
                                tableName, colIndex, settings.dataCounts[tableIndex]));
            }

            // execute query
            logger.info(String.format("    #%d Send to Master: %s", i, sql));
            String[] tmp = checkQuery(client, testDb, sql);
            /*String result = tmp[1];

            // there are more than 1 line in result  & testGroupBy
            if (result.split(System.lineSeparator()).length > 1 && testGroupBy) {
                checkGroupBy(client, testDb, testData, sql, tableName);
            }*/
        }
        logger.trace("============================= Where tests end =============================");
    }

    // I had problem with group by, because databases have different way to group data
    //   - I got different data in different order etc
    /*private void checkGroupBy(Client client, TestDbManager testDb, TestData testData,
                              String sql, String tableName) throws SQLException {
        String[] colNames = testData.getColumnsNames(tableName);
        String sqlBase = sql + " GROUP BY ";
        for (String col : colNames) {
            checkQuery(client, testDb, sqlBase + col);
        }
    }*/

    public void testAdvancedSelectSetUp(ASSettings settings) throws Exception {
        DockerRunner dockerRunner = DockerRunner.getInstance();

        // run Master
        dockerRunner.runMasterInDocker("Master");
        Sleep(5);

        // run Nodes
        DockerThread[] nodes = new DockerThread[settings.nodesCount];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = dockerRunner.runNodeInDocker("Node #" + i);
            Sleep(0, 500);
        }

        Sleep(1);
        // run test on Client side
        DockerThread clientThread = dockerRunner.runTestInDocker
                (AdvancedSelectITest.class, "testAdvancedSelect_Client", settings.toArrayString(), "Client");

        // wait for Client and Nodes end
        clientThread.join();

        dockerRunner.stopThreads();

        // if closed correctly then weren't errors
        assertEquals(0, clientThread.exitVal);
    }


    @Test
    public void testAdvancedSelectV1() throws Exception {
        ASSettings settings = new ASSettings();
        settings.whereCommandCount = 10;
        settings.replicationFactor = 2;
        settings.nodesCount = 4;
        settings.dataCounts = new Integer[]{
                2 * settings.nodesCount, 4 * settings.nodesCount};

        testAdvancedSelectSetUp(settings);
    }

    @Test
    public void testAdvancedSelectV2() throws Exception {
        ASSettings settings = new ASSettings();
        settings.whereCommandCount = 0;
        settings.replicationFactor = 5;
        settings.nodesCount = 10;
        settings.dataCounts = new Integer[]{
                100 * settings.nodesCount, 30 * settings.nodesCount};

        testAdvancedSelectSetUp(settings);
    }

    @Test
    public void testAdvancedSelectV3() throws Exception {
        ASSettings settings = new ASSettings();
        settings.whereCommandCount = 100;
        settings.replicationFactor = 2;
        settings.nodesCount = 10;
        settings.dataCounts = new Integer[]{
                10 * settings.nodesCount, 10 * settings.nodesCount};

        testAdvancedSelectSetUp(settings);
    }

    @After
    public void tearDown() throws Exception {
        DockerRunner.getInstance().stopThreads();
    }


    static class ASSettings extends Settings {
        public int whereCommandCount = 300;

        @Override
        public ASSettings getSettingsFromParams(String[] myParams) {
            super.getSettingsFromParams(myParams);
            whereCommandCount = Integer.parseInt(myParams[defaultParamsCount]);
            return this;
        }

        @Override
        public String[] toArrayString() {
            String[] oldResult = super.toArrayString();
            String[] newResult = Arrays.copyOf(super.toArrayString(),
                    oldResult.length + 1);
            newResult[defaultParamsCount] = String.valueOf(whereCommandCount);

            return newResult;
        }
    }

}
