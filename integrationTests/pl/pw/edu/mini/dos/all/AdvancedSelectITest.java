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
public class AdvancedSelectITest {
    private static final Logger logger = LoggerFactory.getLogger(AdvancedSelectITest.class);

    String configTestFilename1 = "testDbConfig.txt";

    public void testAdvancedSelect_Client(String[] args) throws Exception {
        Client client = new Client(getMasterIpFromParams(args),
                getMasterPortFromParams(args), getMyIpFromParams(args));

        Settings settings = Settings.getSettingsFromParams(getMyParams(args));
        // load command
        TestData testData = TestData.loadConfigTestDbFile(configTestFilename1);
        TestDbManager testDb = new TestDbManager();
        logger.info("cmd=" + testData);
        logger.info("cmd.len=" + testData.insertTableCommands.size());

        // send cmd to Master and run cmd on local test database
        for (String cmd : testData.createTableCommands) {
            runQuery(client, testDb, cmd);
        }
        for (int i = 0; i < settings.dataCount; i++) {
            runQuery(client, testDb, testData.insertTableCommands.get(i));
        }

        // check all data
        String sqlGetAll = "SELECT * FROM " + testData.getTableNames()[0];
        String result = client.executeSQL(sqlGetAll);
        logger.info(result);

        checkDataCorrectness(result, testData.getTableNames()[0], testData, settings.dataCount);
        checkQuery(client, testDb, sqlGetAll);

        // test select with where randomly
        checkWhere(client, testDb, testData, true, settings);

        client.stopClient();
        logger.info("Client end");
    }

    private void checkWhere(Client client, TestDbManager testDb, TestData testData,
                            boolean testGroupBy, Settings settings)
            throws SQLException {
        logger.info("============================== Where tests ==============================");

        long seed = 345;
        Random rand = new Random(seed);
        String sql = "";
        int tableCount = testData.getTableNames().length;

        for (int i = 0; i < settings.whereCommandCount; i++) {

            // get data
            String tableName = testData.getTableNames()[rand.nextInt(tableCount)];
            String[] colNames = testData.getColumnsNames(tableName);
            sql = String.format("SELECT * FROM %s WHERE", tableName);
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
            String[] tmp = checkQuery(client, testDb, sql);
            /*String result = tmp[1];

            // there are more than 1 line in result  & testGroupBy
            if (result.split(System.lineSeparator()).length > 1 && testGroupBy) {
                checkGroupBy(client, testDb, testData, sql, tableName);
            }*/
        }
        logger.info("============================= Where tests end =============================");
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

    public void testAdvancedSelectSetUp(Settings settings) throws Exception {
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
        Settings settings = new Settings();
        settings.whereCommandCount = 10;
        settings.replicationFactor = 2;
        settings.nodesCount = 4;
        settings.dataCount = 2 * settings.nodesCount;

        testAdvancedSelectSetUp(settings);
    }

    @Test
    public void testAdvancedSelectV2() throws Exception {
        Settings settings = new Settings();
        settings.whereCommandCount = 0;
        settings.replicationFactor = 5;
        settings.nodesCount = 10;
        settings.dataCount = 100 * settings.nodesCount;

        testAdvancedSelectSetUp(settings);
    }

    @Test
    public void testAdvancedSelectV3() throws Exception {
        Settings settings = new Settings();
        settings.whereCommandCount = 100;
        settings.replicationFactor = 2;
        settings.nodesCount = 10;
        settings.dataCount = 10 * settings.nodesCount;

        testAdvancedSelectSetUp(settings);
    }

    @After
    public void tearDown() throws Exception {
        DockerRunner.getInstance().stopThreads();
    }


    static class Settings {
        public int whereCommandCount = 300;
        public int replicationFactor = 2;
        public int nodesCount = 10;
        public int dataCount = 10 * nodesCount;

        public static Settings getSettingsFromParams(String[] myParams) {
            Settings settings = new Settings();

            settings.replicationFactor = Integer.parseInt(myParams[0]);
            settings.nodesCount = Integer.parseInt(myParams[1]);
            settings.dataCount = Integer.parseInt(myParams[2]);
            settings.whereCommandCount = Integer.parseInt(myParams[3]);

            return settings;
        }

        public String[] toArrayString() {
            return new String[] {
                    String.valueOf(replicationFactor),
                    String.valueOf(nodesCount),
                    String.valueOf(dataCount),
                    String.valueOf(whereCommandCount)
            };
        }
    }

}