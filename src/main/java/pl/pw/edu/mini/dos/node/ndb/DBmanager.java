package pl.pw.edu.mini.dos.node.ndb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Config;
import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlRequest;
import pl.pw.edu.mini.dos.communication.nodenode.GetSqlResultResponse;

import java.sql.*;
import java.util.List;
import java.util.concurrent.Callable;

/**
 *  Manages the persistant SQLite database of node.
 */
public class DBmanager {
    private static final Logger logger = LoggerFactory.getLogger(DBmanager.class);
    private static final Config config = Config.getConfig();
    private SQLiteDb db;
    private String pathToDBFile;

    public DBmanager(int dbPrefix) {
        pathToDBFile = "jdbc:sqlite:";
        // Random name of the db to not share db with other node
        pathToDBFile +=  dbPrefix + config.getProperty("nodeDatabasePath");
        this.db = SQLiteDb.getInstance();
    }

    /**
     * Create a new Callable that will execute the sql request in a new thread of node.
     *
     * @param executeSqlRequest sql request
     * @return callable sql job
     */
    public Callable<GetSqlResultResponse> newSQLJob(ExecuteSqlRequest executeSqlRequest) {
        if (executeSqlRequest.getSql().matches("(SELECT|select).*")) {
            // Selects
            return new SQLReadJob(db.getConnection(pathToDBFile), executeSqlRequest);
        } else {
            // Creates, Inserts...
            return new SQLWriteJob(db.getConnection(pathToDBFile), executeSqlRequest);
        }
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
                    logger.debug("Table already exists: " + e.getMessage());
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
