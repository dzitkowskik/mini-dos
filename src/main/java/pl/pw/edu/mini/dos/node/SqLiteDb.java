package pl.pw.edu.mini.dos.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Created by ghash on 02.12.2015.
 */
public class SqLiteDb implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(SqLiteDb.class);

    Connection connection;

    public SqLiteDb()  {

        // TODO: get it from config
        String pathToDBFile = "node.db";

        // load the sqlite-JDBC driver using the current class loader
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            logger.error("Cannot find sqlite driver - {}", e.getMessage());
        }

        // connect to db
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + pathToDBFile);
            this.connection.setAutoCommit(true);
        } catch (SQLException e) {
            logger.error("Cannot connect to sqlite database - {}", e.getMessage());
        }
    }

    public int ExecuteQuery(String query) throws SQLException {
        Statement stmt = null;
        try {
            stmt = this.connection.createStatement();
            return stmt.executeUpdate(query);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    @Override
    public void close() {
        try {
            this.connection.close();
            logger.info("Sqlite connection closed");
        } catch (SQLException e) {
            logger.error("Error while closing sqlite connection - ", e.getMessage());
        }
    }
}
