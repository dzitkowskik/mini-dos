package pl.pw.edu.mini.dos.node.ndb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;

import java.sql.*;

/**
 * Provides the required methods to interact with the SQLite data base.
 */
public class SQLiteDb {
    private static final Logger logger = LoggerFactory.getLogger(SQLiteDb.class);
    private static final String DRIVER = "org.sqlite.JDBC";
    private static SQLiteDb instance = null;

    private SQLiteDb(){
    }

    public static SQLiteDb getInstance() {
        if (instance==null){
            instance = new SQLiteDb();
        }
        return instance;
    }

    public Connection getConnection(String dbURL) {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            logger.error("Cannot find sqlite driver - {}", e.getMessage());
        }

        // Connect to db
        Connection conn = null;
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            conn = DriverManager.getConnection(
                    dbURL, config.toProperties());
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            logger.error("Cannot connect to sqlite database - {}", e.getMessage());
        }
        return conn;
    }

    public void rollback(Connection conn) {
        try {
            conn.rollback();
            logger.trace("Rollback OK");
        } catch (SQLException e) {
            logger.error("Rollback failed");
        }
    }

    public void commit(Connection conn) {
        try {
            conn.commit();
            logger.trace("Commit OK");
        } catch (SQLException e) {
            logger.error("Commit failed");
        }
    }

    public void close(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                logger.error("Close Statement failed");
            }
        }
    }

    public void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.error("Close ResultSet failed");
            }
        }
    }

    public void close(Connection conn) {
        try {
            conn.close();
            logger.info("Sqlite connection closed");
        } catch (SQLException e) {
            logger.error("Error while closing sqlite connection: {}", e.getMessage());
        }
    }
}
