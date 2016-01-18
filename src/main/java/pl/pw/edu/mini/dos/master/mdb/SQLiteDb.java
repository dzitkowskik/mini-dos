package pl.pw.edu.mini.dos.master.mdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;

import java.sql.*;

/**
 * Provides the required methods to interact with the SQLite data base.
 * *It uses only one connection because in SQLite it is faster than use
 * many connections or a pool of connections.
 */
public class SQLiteDb {
    private static final Logger logger = LoggerFactory.getLogger(SQLiteDb.class);
    private static final String DRIVER = "org.sqlite.JDBC";
    private Connection connection;

    public SQLiteDb(String dbURL) {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            logger.error("Cannot find sqlite driver - {}", e.getMessage());
        }

        // Connect to db
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            this.connection = DriverManager.getConnection(
                    dbURL, config.toProperties());
            this.connection.setAutoCommit(false);
        } catch (SQLException e) {
            logger.error("Cannot connect to sqlite database - {}", e.getMessage());
        }
    }

    public Statement createStatement() throws SQLException {
        return this.connection.createStatement();
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return this.connection.prepareStatement(sql);
    }

    public void rollback() {
        try {
            connection.rollback();
            logger.trace("Rollback OK");
        } catch (SQLException e) {
            logger.error("Rollback failed");
        }
    }

    public void commit() {
        try {
            connection.commit();
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

    public void close() {
        try {
            this.connection.close();
            logger.trace("Sqlite connection closed");
        } catch (SQLException e) {
            logger.error("Error while closing sqlite connection: {}", e.getMessage());
        }
    }

    public void createBackup(String name){
        try {
            Statement st = connection.createStatement();
            st.executeUpdate("backup to " + name);
        } catch (SQLException e) {
            logger.error("Unable to backup database");
            return;
        }
        logger.trace("Dabatase backup created!");
    }

    public void restoreBackup(String name){
        try {
            Statement st = connection.createStatement();
            st.executeUpdate("restore from " + name);
        } catch (SQLException e) {
            logger.error("Unable to restore backup");
            return;
        }
        logger.trace("Dabatase backup restored!");
    }
}
