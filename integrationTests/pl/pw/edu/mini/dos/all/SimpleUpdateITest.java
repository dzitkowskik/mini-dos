package pl.pw.edu.mini.dos.all;

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.DockerStuff.DockerRunner;
import pl.pw.edu.mini.dos.DockerStuff.DockerThread;
import pl.pw.edu.mini.dos.Utils.SendDataHelper;
import pl.pw.edu.mini.dos.TestData;
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
public class SimpleUpdateITest {
    private static final Logger logger = LoggerFactory.getLogger(SimpleUpdateITest.class);

    String configTestFilename1 = "testDbConfig.txt";

    public void testBasicUpdate_Client(String[] args) throws Exception {
        Client client = new Client(getMasterIpFromParams(args),
                getMasterPortFromParams(args), getMyIpFromParams(args));

        SUSettings settings = new SUSettings().getSettingsFromParams(getMyParams(args));
        // load command
        TestData testData = TestData.loadConfigTestDbFile(configTestFilename1);
        TestDbManager testDb = new TestDbManager();
        SendDataHelper sendHelper = new SendDataHelper(testData, client, testDb);

        logger.trace("cmd=" + testData);
        logger.trace("cmd.len=" + testData.insertTableCommands.size());

        // send command to Master
        sendHelper.sendCreateQueries();
        sendHelper.sendInsertQueriesForAllTables(settings.dataCounts);
        logger.trace("===== end adding data =====");

        logger.info("Checking data...");
        checkDataCorrectnessForAllTable(client, testDb, testData, settings.dataCounts);
        logger.info("Data is OK.");

        logger.info("Checking update queries...");
        checkUpdate(client, testDb, testData, settings);
        logger.info("Test update finished.");

        client.stopClient();
        logger.trace("Client end");
    }

    private void checkUpdate(Client client, TestDbManager testDb,
                             TestData testData, SUSettings settings) throws SQLException {
        logger.trace("============================== Update tests ==============================");

        long seed = 456;
        Random rand = new Random(seed);
        String sql = "";
        int tableCount = testData.getTableNames().length;
        String tableName = "";

        for (int i = 0; i < settings.updateCommandCount; i++) {

            // get data
            int tableIndex = rand.nextInt(tableCount);
            tableName = testData.getTableNames()[tableIndex];
            String[] colNames = testData.getColumnsNames(tableName);

            sql = String.format("SELECT * FROM %s", tableName);
            checkQuery(client, testDb, sql);

            sql = String.format("UPDATE %s SET ", tableName);
            int updateCount = rand.nextInt(4);

            // build query
            // set
            int colIndex = rand.nextInt(colNames.length);
            sql += String.format("%s = \"%s\"", colNames[colIndex],
                    testData.getRandomValueFromColumn(
                            tableName, colIndex, settings.dataCounts[tableIndex]));

            for (int w = 0; w < updateCount; w++) {
                colIndex = rand.nextInt(colNames.length);
                sql += ",";
                sql += String.format(" %s = \"%s\"", colNames[colIndex],
                        testData.getRandomValueFromColumn(
                                tableName, colIndex, settings.dataCounts[tableIndex]));
            }

            // where
            colIndex = rand.nextInt(colNames.length);
            sql += String.format(" WHERE %s = \"%s\"", colNames[colIndex],
                    testData.getRandomValueFromColumn(
                            tableName, colIndex, settings.dataCounts[tableIndex]));

            int whereCount = rand.nextInt(2);
            for (int w = 0; w < whereCount; w++) {
                colIndex = rand.nextInt(colNames.length);
                sql += (rand.nextBoolean() ? " AND" : " OR");
                sql += String.format(" %s = \"%s\"", colNames[colIndex],
                        testData.getRandomValueFromColumn(
                                tableName, colIndex, settings.dataCounts[tableIndex]));
            }

            // execute query
            logger.info(String.format("    #%d Send to Master: %s", i, sql));
            runQuery(client, testDb, sql);

        }

        sql = String.format("SELECT * FROM %s", tableName);
        checkQuery(client, testDb, sql);

        logger.trace("============================= Update tests end =============================");
    }

    public void testBasicUpdateSetUp(SUSettings SUSettings) throws Exception {
        DockerRunner dockerRunner = DockerRunner.getInstance();

        // run Master
        dockerRunner.runMasterInDocker("Master");
        Sleep(5);

        // run Nodes
        DockerThread[] nodes = new DockerThread[SUSettings.nodesCount];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = dockerRunner.runNodeInDocker("Node #" + i);
            Sleep(0, 500);
        }

        Sleep(1);
        // run test on Client side
        DockerThread clientThread = dockerRunner.runTestInDocker
                (SimpleUpdateITest.class, "testBasicUpdate_Client", SUSettings.toArrayString(), "Client");

        // wait for Client and Nodes end
        clientThread.join();

        dockerRunner.stopThreads();

        // if closed correctly then weren't errors
        assertEquals(0, clientThread.exitVal);
    }

    @Test
    public void testBasicUpdateV1() throws Exception {
        SUSettings SUSettings = new SUSettings();
        SUSettings.nodesCount = 4;
        SUSettings.dataCounts = new Integer[]{
                5 * SUSettings.nodesCount, 5 * SUSettings.nodesCount};
        SUSettings.updateCommandCount = 10;

        testBasicUpdateSetUp(SUSettings);
    }

    @Test
    public void testBasicUpdateV2() throws Exception {
        SUSettings SUSettings = new SUSettings();
        SUSettings.nodesCount = 10;
        SUSettings.dataCounts = new Integer[]{
                10 * SUSettings.nodesCount, 10 * SUSettings.nodesCount};
        SUSettings.updateCommandCount = 100;

        testBasicUpdateSetUp(SUSettings);
    }

    @After
    public void tearDown() throws Exception {
        DockerRunner.getInstance().stopThreads();
    }

    static class SUSettings extends Settings {
        public int updateCommandCount;

        @Override
        public SUSettings getSettingsFromParams(String[] myParams) {
            super.getSettingsFromParams(myParams);
            updateCommandCount = Integer.parseInt(myParams[defaultParamsCount]);
            return this;
        }

        @Override
        public String[] toArrayString() {
            String[] oldResult = super.toArrayString();
            String[] newResult = Arrays.copyOf(super.toArrayString(),
                    oldResult.length + 1);
            newResult[defaultParamsCount] = String.valueOf(updateCommandCount);

            return newResult;
        }
    }

}
