package pl.pw.edu.mini.dos.node.ndb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.nodenode.SerializableResultSet;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages the in-memory SQLite database of node.
 */
public class ImDBmanager {
    private static final Logger logger = LoggerFactory.getLogger(ImDBmanager.class);
    private static final String DB_URL = "jdbc:sqlite::memory:";
    private InSQLiteDb imdb;

    public ImDBmanager() {
        this.imdb = new InSQLiteDb(DB_URL);
    }

    public void close() {
        imdb.close();
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
     * Create a new table and insert on it data of the given resultset.
     *
     * @param tableName name of the table to create
     * @param rs        resultset
     * @return true if no errors
     */
    @SuppressWarnings("unchecked")
    public boolean importTable(String tableName, SerializableResultSet rs) {
        logger.debug("Importing table " + tableName + " into in-memory db");
        // Build create table
        String createTable = "CREATE TABLE " + tableName + "(";
        int c;
        for (c = 0; c < rs.getColumnCount() - 2; c++) {
            createTable += rs.getColumnsNames().get(c) + " " + rs.getColumnsTypes().get(c) + ", ";
        }
        // rowID
        createTable += rs.getColumnsNames().get(c) + " " + rs.getColumnsTypes().get(c) + " PRIMARY KEY, ";
        c++;
        // version
        createTable += rs.getColumnsNames().get(c) + " " + rs.getColumnsTypes().get(c);
        createTable += ")WITHOUT ROWID;";
        logger.debug("Create received table: " + createTable);

        //Create table
        List<String> createTableList = new ArrayList<>(1);
        createTableList.add(createTable);
        createTables(createTableList);

        // Prepare insert statement
        String insert = "INSERT INTO " + tableName + " VALUES(?";
        for (int i = 1; i < rs.getColumnCount(); i++) {
            insert += ", ?";
        }
        insert += ");";
        PreparedStatement st;
        try {
            st = imdb.prepareStatement(insert);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            imdb.rollback();
            return false;
        }

        // Insert data
        logger.debug("Importing data into " + tableName);
        Consumer[] functions = DBhelper.getSetDataFunctions(st, rs.getColumnsTypes());
        try {
            for (Object[] row : rs.getData()) {
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
        logger.debug("Data imported into " + tableName);
        return true;
    }

    public boolean mergeVersionsOfTable(String table, List<String> versions, List<String> columnsNames) {
        logger.debug("Merging diferent version into " + table + "_tmp");
        // Build create table
        String createStatement = "CREATE TABLE " + table + "_tmp AS";
        createStatement += " SELECT ";
        for (int i = 0; i < columnsNames.size() - 1; i++) {
            createStatement += columnsNames.get(i) + ", ";
        }
        createStatement += columnsNames.get(columnsNames.size() - 1);
        createStatement += " FROM " + versions.get(0);
        for (int i = 1; i < versions.size(); i++) {
            createStatement += " UNION";
            createStatement += " SELECT ";
            for (int j = 0; j < columnsNames.size() - 1; j++) {
                createStatement += columnsNames.get(j) + ", ";
            }
            createStatement += columnsNames.get(columnsNames.size() - 1);
            createStatement += " FROM " + versions.get(i);
        }
        createStatement += ";";
        logger.debug(createStatement);

        // Create temportal table
        PreparedStatement st = null;
        try {
            st = imdb.prepareStatement(createStatement);
            st.executeUpdate();
        } catch (SQLException e) {
            imdb.rollback();
            logger.error(e.getMessage());
            return false;
        } finally {
            imdb.close(st);
        }
        return true;
    }

    /**
     * Return a SerializableResultSet with the result of execute given select.
     * It doesn't removes the columns rowID and version.
     *
     * @param select select statement
     * @return SerializableResultSet with the result of the select
     */
    public SerializableResultSet executeSelectRaw(String select) {
        logger.debug("Executing " + select);
        List<String> columnsTypes = new ArrayList<>();
        List<String> columnsNames = new ArrayList<>();
        List<Object[]> data = new ArrayList<>();
        // Execute select
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = imdb.prepareStatement(select);
            rs = st.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                columnsTypes.add(meta.getColumnTypeName(i));
                columnsNames.add(meta.getColumnName(i));
            }
            // Save data
            while (rs.next()) {
                Object[] row = new Object[columnsNames.size()];
                for (int i = 1; i <= columnsNames.size(); i++) {
                    row[i - 1] = rs.getObject(i);
                }
                data.add(row);
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        } finally {
            imdb.close(rs);
            imdb.close(st);
        }
        return new SerializableResultSet(columnsTypes, columnsNames, data);
    }

    /**
     * Return a string with the result of execute given select.
     * It removes the columns rowID and version.
     *
     * @param select select statement
     * @return string with the result of the select
     */
    public String executeSelect(String select) {
        SerializableResultSet resultSet = executeSelectRaw(select);
        boolean hasRowIDVersion =
                resultSet.getColumnsNames().get(resultSet.getColumnCount() - 1).equals("version") &&
                        resultSet.getColumnsNames().get(resultSet.getColumnCount() - 1).equals("row_id");
        // Build string
        String result = "\nData (" + resultSet.getData().size() + " rows):";
        for (Object[] o : resultSet.getData()) {
            if (hasRowIDVersion) {
                o = Arrays.copyOfRange(o, 0, resultSet.getColumnCount() - 3);
            }
            result += "\n" + Arrays.toString(o);
        }
        return result;
    }
}