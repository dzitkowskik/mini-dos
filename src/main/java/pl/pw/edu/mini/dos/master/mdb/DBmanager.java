package pl.pw.edu.mini.dos.master.mdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DBmanager {
    private static final Logger logger = LoggerFactory.getLogger(DBmanager.class);
    private static final String DB_URL = "jdbc:sqlite::memory:";
    private SQLiteDb imdb;
    // Prepared statements
    private PreparedStatement newTableInsert;
    private PreparedStatement nextRowIdSelect;
    private PreparedStatement incrementRowIdUpdate;
    private PreparedStatement newRowInsert;
    private PreparedStatement selectCreateTableStatements;

    public DBmanager() {
        this.imdb = new SQLiteDb(DB_URL);
    }

    /**
     * Create needed tables for the in-memory database.
     */
    public void prepareDB() {
        String tablesTable = "" +
                "CREATE TABLE tables(" +
                "table_name			TEXT	PRIMARY KEY NOT NULL, " +
                "create_statement	TEXT				NOT NULL, " +
                "next_row_id		INTEGER				NOT NULL" +
                ")WITHOUT ROWID;";
        String rowsTable = "" +
                "CREATE TABLE rows(" +
                "row_id     INTEGER NOT NULL, " +
                "table_name	TEXT	NOT NULL, " +
                "node_id    INTEGER NOT NULL, " +
                "FOREIGN KEY(table_name) REFERENCES tables(table_name));";

        Statement st = null;
        try {
            st = imdb.createStatement();
            st.executeUpdate(tablesTable);
            st.executeUpdate(rowsTable);
            imdb.commit();
        } catch (SQLException e) {
            logger.error("Error at creating tables: {} - {}", e.getMessage(), e.getStackTrace());
            imdb.rollback();
        } finally {
            imdb.close(st);
        }
        prepareStatements();
    }

    /**
     * Prepare the Statements that will be used to be faster.
     */
    public void prepareStatements() {
        String newTableInsert = "" +
                "INSERT INTO tables (table_name, create_statement, next_row_id) " +
                "VALUES (?,?,0);";
        String nextRowIdSelect = "" +
                "SELECT next_row_id FROM tables WHERE table_name=?;";
        String incrementRowIdUpdate = "" +
                "UPDATE tables SET next_row_id = next_row_id + 1 WHERE table_name=?;";
        String newRowInsert = "" +
                "INSERT INTO rows (row_id, table_name, node_id)" +
                "VALUES (?,?,?);";
        String selectCreateTableStatements = "" +
                "SELECT create_statement FROM tables;";

        try {
            this.newTableInsert = imdb.prepareStatement(newTableInsert);
            this.nextRowIdSelect = imdb.prepareStatement(nextRowIdSelect);
            this.incrementRowIdUpdate = imdb.prepareStatement(incrementRowIdUpdate);
            this.newRowInsert = imdb.prepareStatement(newRowInsert);
            this.selectCreateTableStatements = imdb.prepareStatement(selectCreateTableStatements);
        } catch (SQLException e) {
            logger.error("Error at creating prepared statements: {} - {}",
                    e.getMessage(), e.getStackTrace());
            imdb.rollback();
        }
    }

    /**
     * Register a table in the db
     *
     * @param tableName       table name
     * @param createStatement sql create statement
     * @return result
     */
    public ErrorEnum insertTable(String tableName, String createStatement) {
        try {
            newTableInsert.setString(1, tableName);
            newTableInsert.setString(2, createStatement);
            newTableInsert.executeUpdate();
            imdb.commit();
        } catch (SQLException e) {
            logger.error("Error at registering table: {} - {}",
                    e.getMessage(), e.getStackTrace());
            imdb.rollback();
            return ErrorEnum.REGISTRING_TABLE_ERROR;
        }
        return ErrorEnum.NO_ERROR;
    }

    /**
     * Return a list of strings with the create table statements of all registered tables.
     *
     * @return create statements list
     */
    public List<String> getCreateTableStatements() {
        List<String> createTableStatements = new ArrayList<>();
        ResultSet rs = null;
        try {
            rs = selectCreateTableStatements.executeQuery();
            while (rs.next()) {
                createTableStatements.add(rs.getString(1));
            }
        } catch (SQLException e) {
            logger.error("Error at getting create tables statements: {} - {}",
                    e.getMessage(), e.getStackTrace());
        } finally {
            imdb.close(rs);
        }
        return createTableStatements;
    }

    /**
     * Return a list of strings with the create table statements of the given tables.
     *
     * @param tables list of tables names
     * @return list of create table statements of the given tables
     */
    public List<String> getCreateTableStatements(List<String> tables) {
        List<String> createTableStatements = new ArrayList<>();
        // Build select
        PreparedStatement st = null;
        String select = "" +
                "SELECT create_statement FROM tables " +
                "WHERE table_name=? ";
        for (int i = 1; i < tables.size(); i++) {
            select += "OR table_name=? ";
        }
        select += ";";
        // Execute select
        ResultSet rs = null;
        try {
            st = imdb.prepareStatement(select);
            for (int i = 1; i <= tables.size(); i++) {
                st.setString(i, tables.get(i - 1));
            }
            rs = st.executeQuery();
            while (rs.next()) {
                createTableStatements.add(rs.getString(1));
            }
        } catch (SQLException e) {
            logger.error("Error at getting create tables statements: {} - {}",
                    e.getMessage(), e.getStackTrace());
            return null; // Table not exist
        } finally {
            imdb.close(rs);
            imdb.close(st);
        }
        return createTableStatements;
    }

    /**
     * Insert metadata related with the insert (RowId, TableId, NodesIds).
     *
     * @param tableName tablename
     * @param nodeIds   list of nodes ids where data will be insert
     * @return result
     */
    public ErrorEnum insertRow(String tableName, List<Integer> nodeIds) {
        ResultSet rs = null;
        try {
            // Get rowId
            nextRowIdSelect.setString(1, tableName);
            rs = nextRowIdSelect.executeQuery();
            Long rowId = rs.getLong(1);
            // Update next rowId
            incrementRowIdUpdate.setString(1, tableName);
            incrementRowIdUpdate.executeUpdate();
            // Insert row
            for (Integer nodeId : nodeIds) {
                newRowInsert.setLong(1, rowId);
                newRowInsert.setString(2, tableName);
                newRowInsert.setInt(3, nodeId);
                newRowInsert.executeUpdate();
            }
            imdb.commit();
        } catch (SQLException e) {
            logger.error("Error at registering row: {} - {}",
                    e.getMessage(), e.getStackTrace());
            imdb.rollback();
            return ErrorEnum.TABLE_NOT_EXIST;
        } finally {
            imdb.close(rs);
        }
        return ErrorEnum.NO_ERROR;
    }

    /**
     * Given a list of tables, it return the ids of the nodes
     * which have data of these tables.
     *
     * @param tables list with names of the tables
     * @return list of nodesIds
     */
    public List<Integer> getNodesHaveTables(List<String> tables) {
        List<Integer> nodes = new ArrayList<>();
        // Build select
        PreparedStatement st = null;
        String select = "" +
                "SELECT DISTINCT node_id " +
                "FROM tables " +
                "NATURAL JOIN  rows " +
                "WHERE table_name=? ";
        for (int i = 1; i < tables.size(); i++) {
            select += "OR table_name=? ";
        }
        select += ";";
        // Execute select
        ResultSet rs = null;
        try {
            st = imdb.prepareStatement(select);
            for (int i = 1; i <= tables.size(); i++) {
                st.setString(i, tables.get(i - 1));
            }
            rs = st.executeQuery();
            while (rs.next()) {
                nodes.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            logger.error("Error at getting nodes: {} - {}",
                    e.getMessage(), e.getStackTrace());
            return null; // Table not exist
        } finally {
            imdb.close(rs);
            imdb.close(st);
        }
        return nodes;
    }

    public void close() {
        imdb.close(newTableInsert);
        imdb.close(nextRowIdSelect);
        imdb.close(incrementRowIdUpdate);
        imdb.close(newRowInsert);
        imdb.close(selectCreateTableStatements);
        imdb.close();
    }
}
