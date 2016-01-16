package pl.pw.edu.mini.dos.node.ndb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Config;
import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlRequest;
import pl.pw.edu.mini.dos.communication.nodenode.GetSqlResultResponse;
import pl.pw.edu.mini.dos.communication.nodenode.SerializableResultSet;

import java.sql.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Manages the persistant SQLite database of node.
 */
public class DBmanager {
    private static final Logger logger = LoggerFactory.getLogger(DBmanager.class);
    private static final Config config = Config.getConfig();
    private SQLiteDb db;
    private String pathToDBFile;

    public DBmanager(int dbPrefix) {
        pathToDBFile = "jdbc:sqlite:";
        // Random name of the db to not share db with other node
        pathToDBFile += dbPrefix + config.getProperty("nodeDatabasePath");
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

    /**
     * Insert the data of a result set into a table.
     *
     * @param table table name
     * @param rs    resultset
     * @return true if success
     */
    @SuppressWarnings("unchecked")
    public boolean insertResultSet(String table, SerializableResultSet rs) {
        // Prepare insert statement
        String insert = "INSERT OR REPLACE INTO " + table + " VALUES(?";
        for (int i = 1; i < rs.getColumnCount(); i++) {
            insert += ", ?";
        }
        insert += ");";
        logger.debug("Insert: " + insert);

        Connection conn = null;
        PreparedStatement st;
        try {
            conn = db.getConnection(pathToDBFile);
            st = conn.prepareStatement(insert);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            db.rollback(conn);
            return false;
        }

        // Insert data
        logger.debug("Inserting data into " + table);
        Consumer[] functions = DBhelper.getSetDataFunctions(st, rs.getColumnsTypes());
        try {
            for (Object[] row : rs.getData()) {
                for (int i = 0; i < row.length; i++) {
                    functions[i].accept(row[i]); // st.setXXX(col,val);
                }
                try {
                    st.executeUpdate();
                } catch (SQLException e) {
                    logger.error(e.getMessage());
                    db.rollback(conn);
                    return false;
                }
            }
        } finally {
            db.commit(conn);
            db.close(st);
            db.close(conn);
        }
        logger.debug("Data inserted into " + table);
        return true;
    }

    /**
     * Drop all the given tables if exist.
     * @param tables tables to drop
     * @return true if no errors
     */
    public boolean dropTables(List<String> tables) {
        logger.debug("Dropping tables...");
        Connection conn = null;
        PreparedStatement st = null;
        try {
            conn = db.getConnection(pathToDBFile);
            for (String table : tables) {
                String delete = "DROP TABLE IF EXISTS " + table + ";";
                st = conn.prepareStatement(delete);
                st.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
            db.rollback(conn);
            return false;
        } finally {
            db.commit(conn);
            db.close(st);
            db.close(conn);
        }
        logger.debug("Tables dropped.");
        return true;
    }
}