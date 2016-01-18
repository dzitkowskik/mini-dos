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
public class SimpleDeleteITest {
    private static final Logger logger = LoggerFactory.getLogger(SimpleDeleteITest.class);

    String configTestFilename1 = "testDbConfig.txt";

    public void testBasicDelete_Client(String[] args) throws Exception {
        Client client = new Client(getMasterIpFromParams(args),
                getMasterPortFromParams(args), getMyIpFromParams(args));

        SDSettings settings = new SDSettings().getSettingsFromParams(getMyParams(args));
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

        logger.info("Checking delete queries...");
        checkDelete(client, testDb, testData, settings);
        logger.info("Test delete finished.");

        client.stopClient();
        logger.trace("Client end");
    }

    private void checkDelete(Client client, TestDbManager testDb,
                             TestData testData, SDSettings settings) throws SQLException {
        logger.trace("============================== Delete tests ==============================");

        long seed = 345;
        Random rand = new Random(seed);
        String sql = "";
        int tableCount = testData.getTableNames().length;
        String tableName = "";

        for (int i = 0; i < settings.deleteCommandCount; i++) {

            // get data
            int tableIndex = rand.nextInt(tableCount);
            tableName = testData.getTableNames()[tableIndex];
            String[] colNames = testData.getColumnsNames(tableName);

            sql = String.format("SELECT * FROM %s", tableName);
            checkQuery(client, testDb, sql);

            sql = String.format("DELETE FROM %s WHERE", tableName);
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
            runQuery(client, testDb, sql);

        }

        sql = String.format("SELECT * FROM %s", tableName);
        checkQuery(client, testDb, sql);

        logger.trace("============================= Delete tests end =============================");
    }

    public void testBasicDeleteSetUp(SDSettings settings) throws Exception {
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
                (SimpleDeleteITest.class, "testBasicDelete_Client", settings.toArrayString(), "Client");

        // wait for Client and Nodes end
        clientThread.join();

        dockerRunner.stopThreads();

        // if closed correctly then weren't errors
        assertEquals(0, clientThread.exitVal);
    }

    @Test
    public void testBasicDeleteV1() throws Exception {
        SDSettings settings = new SDSettings();
        settings.nodesCount = 4;
        settings.dataCounts =
                new Integer[] {5 * settings.nodesCount, 5 * settings.nodesCount};
        settings.deleteCommandCount = 10;

        testBasicDeleteSetUp(settings);
    }

    @Test
    public void testBasicDeleteV2() throws Exception {
        SDSettings settings = new SDSettings();
        settings.nodesCount = 10;
        settings.dataCounts =
                new Integer[] {20 * settings.nodesCount, 20 * settings.nodesCount};
        settings.deleteCommandCount = 100;

        testBasicDeleteSetUp(settings);
    }

    @After
    public void tearDown() throws Exception {
        DockerRunner.getInstance().stopThreads();
    }

    static class SDSettings extends Settings {
        public int deleteCommandCount;

        @Override
        public SDSettings getSettingsFromParams(String[] myParams) {
            super.getSettingsFromParams(myParams);
            deleteCommandCount = Integer.parseInt(myParams[defaultParamsCount]);
            return this;
        }

        @Override
        public String[] toArrayString() {
            String[] oldResult = super.toArrayString();
            String[] newResult = Arrays.copyOf(super.toArrayString(),
                    oldResult.length + 1);
            newResult[defaultParamsCount] = String.valueOf(deleteCommandCount);

            return newResult;
        }
    }

}
