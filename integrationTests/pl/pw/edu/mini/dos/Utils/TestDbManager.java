package pl.pw.edu.mini.dos.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.nodenode.SerializableResultSet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: asd
 * Date: 1/9/16
 * Time: 3:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestDbManager {
    static final Logger logger = LoggerFactory.getLogger(TestDbManager.class);
    static final String DRIVER = "org.sqlite.JDBC";
    static final String URLprefix = "jdbc:sqlite:";
    String dbURL;

    Connection c;
    Statement stmt;

    public TestDbManager() {
        try {
            Class.forName(DRIVER);
            dbURL = URLprefix + "test_"
                    + new Random().nextInt(10000) + ".db";
            c = DriverManager.getConnection(dbURL);
        } catch (ClassNotFoundException | SQLException e) {
            logger.error(e.getMessage());
        }
    }

    public SerializableResultSet executeQuery(String sql) throws SQLException {
        List<String> columnsTypes = new ArrayList<>();
        List<String> columnsNames = new ArrayList<>();
        List<Object[]> data = new ArrayList<>();
        ResultSet rs = null;
        PreparedStatement pst = null;

        try {
            pst = c.prepareStatement(sql);
            rs = pst.executeQuery();
            // Save columns types and names
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            for (int col = 1; col <= cols; col++) {
                columnsTypes.add(meta.getColumnTypeName(col));
                columnsNames.add(meta.getColumnName(col));
            }
            // Save data
            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int col = 1; col <= cols; col++) {
                    row[col - 1] = rs.getObject(col);
                }
                data.add(row);
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (pst != null) {
                pst.close();
            }
        }
        return new SerializableResultSet(columnsTypes, columnsNames, data);

    }

    public int executeUpdate(String sql) {
        try {
            stmt = c.createStatement();
            int rowsAffected = stmt.executeUpdate(sql);
            stmt.close();

            return rowsAffected;
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        return -1;
    }

}
