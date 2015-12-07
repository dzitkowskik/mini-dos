package pl.pw.edu.mini.dos.node.ndb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Config;
import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlRequest;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Random;

public class DBmanager {
    private static final Logger logger = LoggerFactory.getLogger(DBmanager.class);
    private static final Config config = Config.getConfig();
    private SQLiteDb db;
    private String pathToDBFile;

    public DBmanager(boolean inMemory) {
        Random rn = new Random();
        pathToDBFile = "jdbc:sqlite:";
        if (inMemory) {
            pathToDBFile += ":memory:";
        } else {
            pathToDBFile += config.getProperty("nodeDatabasePath") + rn.nextInt();
        }
        this.db = SQLiteDb.getInstance();
    }

    public SQLiteJob newSQLiteJob(ExecuteSqlRequest executeSqlRequest) {
        return new SQLiteJob(db.getConnection(pathToDBFile), executeSqlRequest);
    }

    /**
     * Execute the given create table statements in local db.
     *
     * @param createTableStatements create table statements
     * @return true: success
     */
    public boolean createTables(List<String> createTableStatements) {
        Connection conn = null;
        Statement st = null;
        try {
            conn = db.getConnection(pathToDBFile);
            st = conn.createStatement();
            for (String createTableStatement : createTableStatements) {
                try {
                    st.executeUpdate(createTableStatement);
                } catch (SQLException e) {
                    logger.debug("Table already exists: " + e.getMessage().toString());
                }
            }
        } catch (SQLException e) {
            db.rollback(conn);
            logger.error(e.getMessage());
            return false;
        } finally {
            db.commit(conn);
            db.close(st);
            db.close(conn);
        }
        return true;
    }
}
