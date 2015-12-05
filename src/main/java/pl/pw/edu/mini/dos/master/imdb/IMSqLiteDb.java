package pl.pw.edu.mini.dos.master.imdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class IMSqLiteDb implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(IMSqLiteDb.class);
    private Connection connection;

    public IMSqLiteDb() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            logger.error("Cannot find sqlite driver - {}", e.getMessage());
        }

        // Connect to db
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite::memory:");
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
            logger.info("Rollback OK");
        } catch (SQLException e) {
            logger.error("Rollback failed");
        }
    }

    public void commit() throws SQLException {
        connection.commit();
        logger.info("Commit OK");
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
            logger.info("Sqlite connection closed");
        } catch (SQLException e) {
            logger.error("Error while closing sqlite connection - ", e.getMessage());
        }
    }
}
