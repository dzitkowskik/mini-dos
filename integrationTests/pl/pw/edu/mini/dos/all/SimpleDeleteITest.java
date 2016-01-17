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
public class SimpleDeleteITest {
    private static final Logger logger = LoggerFactory.getLogger(SimpleDeleteITest.class);

    String configTestFilename1 = "testDbConfig.txt";

    public void testBasicDelete_Client(String[] args) throws Exception {
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
            runQuery(client, testDb, cmd);
        }
        for (int i = 0; i < settings.dataCount; i++) {
            runQuery(client, testDb, testData.insertTableCommands.get(i));
        }
        logger.trace("===== end adding data =====");
        String result = client.executeSQL("SELECT * FROM " + testData.getTableNames()[0]);
        logger.trace(result);

        checkDataCorrectness(result, testData.getTableNames()[0], testData, settings.dataCount);

        checkDelete(client, testDb, testData, settings);

        client.stopClient();
        logger.trace("Client end");
    }

    private void checkDelete(Client client, TestDbManager testDb,
                             TestData testData, Settings settings) throws SQLException {
        logger.trace("============================== Delete tests ==============================");

        long seed = 345;
        Random rand = new Random(seed);
        String sql = "";
        int tableCount = testData.getTableNames().length;
        String tableName = "";

        for (int i = 0; i < settings.deleteCommandCount; i++) {

            // get data
            tableName = testData.getTableNames()[rand.nextInt(tableCount)];
            String[] colNames = testData.getColumnsNames(tableName);

            sql = String.format("SELECT * FROM %s", tableName);
            checkQuery(client, testDb, sql);

            sql = String.format("DELETE FROM %s WHERE", tableName);
            int whereCount = rand.nextInt(3);

            // build query
            int colIndex = rand.nextInt(colNames.length);
            sql += String.format(" %s = \"%s\"", colNames[colIndex],
                    testData.getRandomValueFromColumn(colIndex, settings.dataCount));

            for (int w = 0; w < whereCount; w++) {
                colIndex = rand.nextInt(colNames.length);
                sql += (rand.nextBoolean() ? " AND" : " OR");
                sql += String.format(" %s = \"%s\"", colNames[colIndex],
                        testData.getRandomValueFromColumn(colIndex, settings.dataCount));
            }

            // execute query
            runQuery(client, testDb, sql);

        }

        sql = String.format("SELECT * FROM %s", tableName);
        checkQuery(client, testDb, sql);

        logger.trace("============================= Delete tests end =============================");
    }

    public void testBasicDeleteSetUp(Settings settings) throws Exception {
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
        Settings settings = new Settings();
        settings.nodesCount = 4;
        settings.dataCount = 5 * settings.nodesCount;
        settings.deleteCommandCount = 10;

        testBasicDeleteSetUp(settings);
    }

    @Test
    public void testBasicDeleteV2() throws Exception {
        Settings settings = new Settings();
        settings.nodesCount = 10;
        settings.dataCount = 20 * settings.nodesCount;
        settings.deleteCommandCount = 100;

        testBasicDeleteSetUp(settings);
    }

    @After
    public void tearDown() throws Exception {
        DockerRunner.getInstance().stopThreads();
    }

    static class Settings {
        public int replicationFactor = 2;
        public int nodesCount;
        public int dataCount;
        public int deleteCommandCount;

        public static Settings getSettingsFromParams(String[] myParams) {
            Settings settings = new Settings();

            settings.replicationFactor = Integer.parseInt(myParams[0]);
            settings.nodesCount = Integer.parseInt(myParams[1]);
            settings.dataCount = Integer.parseInt(myParams[2]);
            settings.deleteCommandCount = Integer.parseInt(myParams[3]);

            return settings;
        }

        public String[] toArrayString() {
            return new String[] {
                    String.valueOf(replicationFactor),
                    String.valueOf(nodesCount),
                    String.valueOf(dataCount),
                    String.valueOf(deleteCommandCount)
            };
        }
    }

}
