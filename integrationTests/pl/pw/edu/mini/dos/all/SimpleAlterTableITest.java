package pl.pw.edu.mini.dos.all;

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.DockerStuff.DockerRunner;
import pl.pw.edu.mini.dos.DockerStuff.DockerThread;
import pl.pw.edu.mini.dos.Utils.SendDataHelper;
import pl.pw.edu.mini.dos.TestData;
import pl.pw.edu.mini.dos.Utils.TestDbManager;
import pl.pw.edu.mini.dos.client.Client;

import java.sql.SQLException;
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
public class SimpleAlterTableITest {
    private static final Logger logger = LoggerFactory.getLogger(SimpleAlterTableITest.class);

    String configTestFilename1 = "testDbConfig.txt";

    String[] colType = {"TEXT",
            "NUMERIC",
            "INTEGER",
            "REAL",
            "BLOB"};

    public void testSimpleAlterTable_Client(String[] args) throws Exception {
        Client client = new Client(getMasterIpFromParams(args),
                getMasterPortFromParams(args), getMyIpFromParams(args));

        Settings settings = Settings.getSettingsFromParams(getMyParams(args));
        // load command
        TestData testData = TestData.loadConfigTestDbFile(configTestFilename1);
        TestDbManager testDb = new TestDbManager();
        SendDataHelper sendHelper = new SendDataHelper(testData, client, testDb);

        logger.trace("cmd=" + testData);
        logger.trace("cmd.len=" + testData.insertTableCommands.size());

        // send cmd to Master and run cmd on local test database
        sendHelper.sendCreateQueries();
        sendHelper.sendInsertQueriesForAllTables(settings.dataCount);

        // check all data
        logger.info("Checking data...");
        String sqlGetAll = "SELECT * FROM " + testData.getTableNames()[0];
        String result = client.executeSQL(sqlGetAll);
        logger.trace(result);

        checkDataCorrectness(result, testData.getTableNames()[0], testData, settings.dataCount);
        checkQuery(client, testDb, sqlGetAll);
        logger.info("Data is OK.");

        // test alter table randomly
        logger.info("Checking alter table quries...");
        checkAlterTable(client, testDb, testData, true, settings);
        logger.info("Test alter table finished.");

        client.stopClient();
        logger.trace("Client end");
    }

    private void checkAlterTable(Client client, TestDbManager testDb, TestData testData,
                                 boolean testGroupBy, Settings settings)
            throws SQLException {
        logger.trace("============================== alter table tests ==============================");

        long seed = 345;
        Random rand = new Random(seed);
        String sql = "";
        int tableCount = testData.getTableNames().length;

        for (int i = 0; i < settings.alterCommandCount; i++) {

            // get data
            String tableName = testData.getTableNames()[rand.nextInt(tableCount)];
            String[] colNames = testData.getColumnsNames(tableName);

            // build query
            if (rand.nextBoolean()) {   // ADD
                String newColName = "NewColName" + rand.nextInt(10000);
                String newColType = colType[rand.nextInt(colType.length)];
                sql = String.format("ALTER TABLE %s %s %s %s", tableName,
                        "ADD COLUMN",
                        //(rand.nextBoolean() ? "ADD" : "ADD COLUMN"), // "ADD" <=> "ADD COLUMN"
                        newColName, newColType);
            } else {    // RENAME
                String newName = tableName + rand.nextInt(1000);
                sql = String.format("ALTER TABLE %s RENAME TO %s",
                        tableName, newName);
            }

            // execute query
            logger.info(String.format("    #%d Send to Master: %s", i, sql));
            runQuery(client, testDb, sql);
        }
        logger.trace("============================= alter table tests end =============================");
    }

    public void testSimpleAlterTableSetUp(Settings settings) throws Exception {
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
                (SimpleAlterTableITest.class, "testSimpleAlterTable_Client", settings.toArrayString(), "Client");

        // wait for Client and Nodes end
        clientThread.join();

        dockerRunner.stopThreads();

        // if closed correctly then weren't errors
        assertEquals(0, clientThread.exitVal);
    }


    @Test
    public void testSimpleAlterTableV1() throws Exception {
        Settings settings = new Settings();
        settings.alterCommandCount = 10;
        settings.replicationFactor = 2;
        settings.nodesCount = 4;
        settings.dataCount = 2 * settings.nodesCount;

        testSimpleAlterTableSetUp(settings);
    }

    @Test
    public void testSimpleAlterTableV2() throws Exception {
        Settings settings = new Settings();
        settings.alterCommandCount = 20;
        settings.replicationFactor = 5;
        settings.nodesCount = 10;
        settings.dataCount = 100 * settings.nodesCount;

        testSimpleAlterTableSetUp(settings);
    }

    @After
    public void tearDown() throws Exception {
        DockerRunner.getInstance().stopThreads();
    }


    static class Settings {
        public int alterCommandCount = 300;
        public int replicationFactor = 2;
        public int nodesCount = 10;
        public int dataCount = 10 * nodesCount;

        public static Settings getSettingsFromParams(String[] myParams) {
            Settings settings = new Settings();

            settings.replicationFactor = Integer.parseInt(myParams[0]);
            settings.nodesCount = Integer.parseInt(myParams[1]);
            settings.dataCount = Integer.parseInt(myParams[2]);
            settings.alterCommandCount = Integer.parseInt(myParams[3]);

            return settings;
        }

        public String[] toArrayString() {
            return new String[]{
                    String.valueOf(replicationFactor),
                    String.valueOf(nodesCount),
                    String.valueOf(dataCount),
                    String.valueOf(alterCommandCount)
            };
        }
    }

}
