package pl.pw.edu.mini.dos.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Helper;
import pl.pw.edu.mini.dos.TestData;
import pl.pw.edu.mini.dos.client.Client;

import java.sql.SQLException;
import java.util.Arrays;

import static pl.pw.edu.mini.dos.Utils.TestsHelper.runQuery;

/**
 * Created by asd on 1/18/16.
 */
public class SendDataHelper {
    private static final Logger logger = LoggerFactory.getLogger(TestsHelper.class);
    TestData testData;
    Client client;
    TestDbManager testDb;

    public SendDataHelper(TestData testData, Client client, TestDbManager testDb) {
        this.testData = testData;
        this.client = client;
        this.testDb = testDb;
    }

    public void sendCreateQueries() throws SQLException {
        for (String cmd : testData.createTableCommands.values()) {
            logger.info("Send to Master: " + cmd);
            runQuery(client, testDb, cmd);
        }
    }

    public void sendCreateQueries(String[] tableNames) throws SQLException {
        for (int i = 0; i < tableNames.length; i++) {
            String cmd = testData.createTableCommands.get(tableNames[i]);
            logger.info("Send to Master: " + cmd);
            runQuery(client, testDb, cmd);
        }
    }

    public void sendInsertQueriesForAllTables(int dataCount) throws SQLException {
        Integer[] dataCounts = new Integer[testData.getTableNames().length];
        Arrays.fill(dataCounts, dataCount);

        sendInsertQueriesForAllTables(dataCounts);
    }

    public void sendInsertQueriesForAllTables(Integer dataCount[]) throws SQLException {
        String tableNames[] = testData.getTableNames();
        logger.trace("tableNames['family']=" + Helper.collectionToString(testData.insertTableCommands.get(tableNames[1])));
        for (int t = 0; t < dataCount.length; t++) {
            logger.trace("Send queries for table " + tableNames[t]);
            for (int i = 0; i < dataCount[t]; i++) {
                logger.info(String.format("#%d Send to Master: %s", i,
                        testData.insertTableCommands.get(tableNames[t]).get(i)));
                runQuery(client, testDb,
                        testData.insertTableCommands.get(tableNames[t]).get(i));
            }
        }
    }

    public void sendInsertQueriesFor(String[] tableNames, int dataCount[]) throws SQLException {
        for (int t = 0; t < tableNames.length; t++) {
            for (int i = 0; i < dataCount[t]; i++) {
                logger.info(String.format("#%d Send to Master: %s", i,
                        testData.insertTableCommands.get(i)));
                runQuery(client, testDb,
                        testData.insertTableCommands.get(tableNames[t]).get(i));
            }
        }
    }
}
