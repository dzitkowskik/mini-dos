package pl.pw.edu.mini.dos.node.ndb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages the in-memory SQLite database of node.
 */
public class InDBmanager {
    private static final Logger logger = LoggerFactory.getLogger(InDBmanager.class);
    private static final String DB_URL = "jdbc:sqlite::memory:";
    private InSQLiteDb imdb;

    public InDBmanager() {
        this.imdb = new InSQLiteDb(DB_URL);
    }

    /**
     * Execute the given create table statements in local db.
     *
     * @param createTableStatements create table statements
     * @return true: success
     */
    public boolean createTables(List<String> createTableStatements) {
        Statement st = null;
        try {
            st = imdb.createStatement();
            for (String createTableStatement : createTableStatements) {
                try {
                    st.executeUpdate(createTableStatement);
                } catch (SQLException e) {
                    logger.debug("Table already exists: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            imdb.rollback();
            logger.error(e.getMessage());
            return false;
        } finally {
            imdb.commit();
            imdb.close(st);
        }
        return true;
    }

    /**
     * Create a new table and insert on it given data.
     *
     * @param tableName    name of the table to create
     * @param columnsTypes table columns types
     * @param data         data to insert
     * @return true if no errors
     */
    public boolean importTable(String tableName, List<String> columnsTypes, List<Object[]> data) {
        // Build create table
        String createTable = "CREATE TABLE " + tableName + "(";
        int c;
        for (c = 0; c < columnsTypes.size() - 2; c++) {
            createTable += "c" + c + " " + columnsTypes.get(c) + ", ";
        }
        createTable += "c" + c + " " + columnsTypes.get(c) + " PRIMARY KEY, "; // rowID
        c++;
        createTable += "c" + c + " " + columnsTypes.get(c); // version
        createTable += ")WITHOUT ROWID;";
        logger.debug("Create table: " + createTable);

        //Create table
        List<String> createTableList = new ArrayList<>(1);
        createTableList.add(createTable);
        createTables(createTableList);

        // Prepare insert statement
        String insert = "INSERT INTO " + tableName + " VALUES(?";
        for (int i = 1; i < columnsTypes.size(); i++) {
            insert += ", ?";
        }
        insert += ");";
        PreparedStatement st;
        try {
            st = imdb.prepareStatement(insert);
        } catch (SQLException e) {
            imdb.rollback();
            logger.error(e.getMessage());
            return false;
        }

        // Insert data
        Consumer[] functions = getFunctions(st, columnsTypes);
        try {
            for (Object[] row : data) {
                for (int i = 0; i < row.length; i++) {
                    functions[i].accept(row[i]); // st.setXXX(col,val);
                }
                st.executeUpdate();
            }
        } catch (SQLException e) {
            imdb.rollback();
            logger.error(e.getMessage());
            return false;
        } finally {
            imdb.commit();
            imdb.close(st);
        }
        return true;
    }

    /**
     * Given the types of each colums returns an array of functions with the function
     * to add to the PrepareStatement the data.
     * Ej: if columnType = TEXT --> st.setString(c, data)
     * Datatypes supported: SQLite3 (https://www.sqlite.org/datatype3.html)
     *
     * @param st           PrepareStatement
     * @param columnsTypes list with types of each column
     * @return array of Lambda functions
     */
    private Consumer[] getFunctions(final PreparedStatement st, List<String> columnsTypes) {
        Consumer[] functions = new Consumer[columnsTypes.size()];
        for (int i = 0; i < columnsTypes.size(); i++) {
            String type = columnsTypes.get(i);
            final int col = i + 1;
            switch (type) {
                case "INTEGER":
                case "INT":
                case "TINYINT":
                case "SMALLINT":
                case "MEDIUMINT":
                case "BIGINT":
                case "INT2":
                case "INT8":
                    functions[i] = x -> {
                        try {
                            st.setLong(col, Long.parseLong(x.toString()));
                        } catch (SQLException e) {
                            logger.error(e.getMessage());
                        }
                    };
                    break;
                case "TEXT":
                case "CHARACTER":
                case "VARCHAR":
                case "CLOB":
                    functions[i] = x -> {
                        try {
                            st.setString(col, x.toString());
                        } catch (SQLException e) {
                            logger.error(e.getMessage());
                        }
                    };
                    break;
                case "BLOB":
                    functions[i] = x -> {
                        try {
                            st.setBlob(col, (Blob) x);
                        } catch (SQLException e) {
                            logger.error(e.getMessage());
                        }
                    };
                    break;
                case "REAL":
                case "DOUBLE":
                case "FLOAT ":
                    functions[i] = x -> {
                        try {
                            st.setDouble(col, Double.parseDouble(x.toString()));
                        } catch (SQLException e) {
                            logger.error(e.getMessage());
                        }
                    };
                    break;
                case "NUMERIC":
                case "DECIMAL":
                case "BOOLEAN":
                case "DATE":
                case "DATETIME ":
                    functions[i] = x -> {
                        try {
                            st.setBigDecimal(col, (BigDecimal) x);
                        } catch (SQLException e) {
                            logger.error(e.getMessage());
                        }
                    };
                    break;
            }
        }
        return functions;
    }

    /**
     * Return a string with the result of selectAll of a table.
     *
     * @param table table
     */
    public String selectAll(String table) {
        List<Object[]> data = new ArrayList<>();
        // Execute select
        PreparedStatement st = null;
        ResultSet rs = null;
        String select = "SELECT * FROM " + table + ";";
        try {
            st = imdb.prepareStatement(select);
            rs = st.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            // Save data
            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int c = 1; c <= cols; c++) {
                    row[c - 1] = rs.getObject(c);
                }
                data.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            imdb.close(rs);
            imdb.close(st);
        }

        // Build string
        String str = ">>> In-memory db: table " + table;
        str += "\nData (" + data.size() + " rows):";
        for (Object[] o : data) {
            str += "\n" + Arrays.toString(o);
        }
        return str;
    }

    public void close() {
        imdb.close();
    }
}