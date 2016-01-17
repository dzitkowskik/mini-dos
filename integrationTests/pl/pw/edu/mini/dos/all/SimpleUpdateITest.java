package pl.pw.edu.mini.dos.all;

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.DockerStuff.DockerRunner;
import pl.pw.edu.mini.dos.DockerStuff.DockerThread;
import pl.pw.edu.mini.dos.TestData;
import pl.pw.edu.mini.dos.client.Client;

import java.sql.SQLException;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static pl.pw.edu.mini.dos.TestsHelper.*;

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

        Settings settings = Settings.getSettingsFromParams(getMyParams(args));
        // load command
        TestData testData = TestData.loadConfigTestDbFile(configTestFilename1);
        TestDbManager testDb = new TestDbManager();
        logger.trace("cmd=" + testData);
        logger.trace("cmd.len=" + testData.insertTableCommands.size());

        // send command to Master
        for (String cmd : testData.createTableCommands) {
            logger.info("Send to Master: " + cmd);
            runQuery(client, testDb, cmd);
        }
        for (int i = 0; i < settings.dataCount; i++) {
            logger.info(String.format("#%d Send to Master: %s", i,
                    testData.insertTableCommands.get(i)));
            runQuery(client, testDb, testData.insertTableCommands.get(i));
        }
        logger.trace("===== end adding data =====");

        logger.info("Checking data...");
        String result = client.executeSQL("SELECT * FROM " + testData.getTableNames()[0]);
        logger.trace(result);

        checkDataCorrectness(result, testData.getTableNames()[0], testData, settings.dataCount);
        logger.info("Data is OK.");

        logger.info("Checking update queries...");
        checkUpdate(client, testDb, testData, settings);
        logger.info("Test update finished.");

        client.stopClient();
        logger.trace("Client end");
    }

    private void checkUpdate(Client client, TestDbManager testDb,
                             TestData testData, Settings settings) throws SQLException {
        logger.trace("============================== Update tests ==============================");

        long seed = 456;
        Random rand = new Random(seed);
        String sql = "";
        int tableCount = testData.getTableNames().length;
        String tableName = "";

        for (int i = 0; i < settings.updateCommandCount; i++) {

            // get data
            tableName = testData.getTableNames()[rand.nextInt(tableCount)];
            String[] colNames = testData.getColumnsNames(tableName);

            sql = String.format("SELECT * FROM %s", tableName);
            checkQuery(client, testDb, sql);

            sql = String.format("UPDATE %s SET ", tableName);
            int updateCount = rand.nextInt(4);

            // build query
            // set
            int colIndex = rand.nextInt(colNames.length);
            sql += String.format("%s = \"%s\"", colNames[colIndex],
                    testData.getRandomValueFromColumn(colIndex, settings.dataCount));

            for (int w = 0; w < updateCount; w++) {
                colIndex = rand.nextInt(colNames.length);
                sql += ",";
                sql += String.format(" %s = \"%s\"", colNames[colIndex],
                        testData.getRandomValueFromColumn(colIndex, settings.dataCount));
            }

            // where
            colIndex = rand.nextInt(colNames.length);
            sql += String.format(" WHERE %s = \"%s\"", colNames[colIndex],
                    testData.getRandomValueFromColumn(colIndex, settings.dataCount));

            int whereCount = rand.nextInt(2);
            for (int w = 0; w < updateCount; w++) {
                colIndex = rand.nextInt(colNames.length);
                sql += (rand.nextBoolean() ? " AND" : " OR");
                sql += String.format(" %s = \"%s\"", colNames[colIndex],
                        testData.getRandomValueFromColumn(colIndex, settings.dataCount));
            }

            // execute query
            logger.info(String.format("    #%d Send to Master: %s", i, sql));
            runQuery(client, testDb, sql);

        }

        sql = String.format("SELECT * FROM %s", tableName);
        checkQuery(client, testDb, sql);

        logger.trace("============================= Update tests end =============================");
    }

    public void testBasicUpdateSetUp(Settings settings) throws Exception {
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
                (SimpleUpdateITest.class, "testBasicUpdate_Client", settings.toArrayString(), "Client");

        // wait for Client and Nodes end
        clientThread.join();

        dockerRunner.stopThreads();

        // if closed correctly then weren't errors
        assertEquals(0, clientThread.exitVal);
    }

    @Test
    public void testBasicUpdateV1() throws Exception {
        Settings settings = new Settings();
        settings.nodesCount = 4;
        settings.dataCount = 5 * settings.nodesCount;
        settings.updateCommandCount = 10;

        testBasicUpdateSetUp(settings);
    }

    @Test
    public void testBasicUpdateV2() throws Exception {
        Settings settings = new Settings();
        settings.nodesCount = 10;
        settings.dataCount = 10 * settings.nodesCount;
        settings.updateCommandCount = 100;

        testBasicUpdateSetUp(settings);
    }

    @After
    public void tearDown() throws Exception {
        DockerRunner.getInstance().stopThreads();
    }

    static class Settings {
        public int replicationFactor = 2;
        public int nodesCount;
        public int dataCount;
        public int updateCommandCount;

        public static Settings getSettingsFromParams(String[] myParams) {
            Settings settings = new Settings();

            settings.replicationFactor = Integer.parseInt(myParams[0]);
            settings.nodesCount = Integer.parseInt(myParams[1]);
            settings.dataCount = Integer.parseInt(myParams[2]);
            settings.updateCommandCount = Integer.parseInt(myParams[3]);

            return settings;
        }

        public String[] toArrayString() {
            return new String[] {
                    String.valueOf(replicationFactor),
                    String.valueOf(nodesCount),
                    String.valueOf(dataCount),
                    String.valueOf(updateCommandCount)
            };
        }
    }

}
