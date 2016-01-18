package pl.pw.edu.mini.dos.master.mdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Helper;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.master.node.RegisteredNode;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBmanager {
    private static final Logger logger = LoggerFactory.getLogger(DBmanager.class);
    private static final String DB_URL = "jdbc:sqlite::memory:";
    private SQLiteDb imdb;
    // Prepared statements
    private PreparedStatement newTableInsert;
    private PreparedStatement nextRowIdSelect;
    private PreparedStatement incrementRowIdUpdate;
    private PreparedStatement newRowInsert;
    private PreparedStatement selectTableNames;
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
                "table_name			TEXT	            NOT NULL, " +
                "create_statement	TEXT				NOT NULL, " +
                "next_row_id		INTEGER				NOT NULL" +
                ");";
        String rowsTable = "" +
                "CREATE TABLE rows(" +
                "row_id     INTEGER NOT NULL, " +
                "table_name	TEXT	NOT NULL, " +
                "node_id    INTEGER NOT NULL " +
                ");";

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
    private void prepareStatements() {
        String newTableInsert = "" +
                "INSERT INTO tables (table_name, create_statement, next_row_id) " +
                "VALUES (?,?,0);";
        String nextRowIdSelect = "" +
                "SELECT next_row_id FROM tables WHERE table_name=?;";
        String incrementRowIdUpdate = "" +
                "UPDATE tables SET next_row_id = next_row_id + 1 WHERE table_name=?;";
        String newRowInsert = "" +
                "INSERT OR REPLACE INTO rows (row_id, table_name, node_id)" +
                "VALUES (?,?,?);";
        String selectTableNames = "" +
                "SELECT table_name FROM tables;";
        String selectCreateTableStatements = "" +
                "SELECT create_statement FROM tables;";

        try {
            this.newTableInsert = imdb.prepareStatement(newTableInsert);
            this.nextRowIdSelect = imdb.prepareStatement(nextRowIdSelect);
            this.incrementRowIdUpdate = imdb.prepareStatement(incrementRowIdUpdate);
            this.newRowInsert = imdb.prepareStatement(newRowInsert);
            this.selectTableNames = imdb.prepareStatement(selectTableNames);
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
     * Return a list of strings with all table names tables.
     *
     * @return create statements list
     */
    public List<String> getTableNames() {
        List<String> tableNames = new ArrayList<>();
        ResultSet rs = null;
        try {
            rs = selectTableNames.executeQuery();
            while (rs.next()) {
                tableNames.add(rs.getString(1));
            }
        } catch (SQLException e) {
            logger.error("Error at getting table names: {} - {}",
                    e.getMessage(), e.getStackTrace());
        } finally {
            imdb.close(rs);
        }
        return tableNames;
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
     * @return rowid
     */
    public synchronized Long insertRow(String tableName, List<Integer> nodeIds) {
        ResultSet rs = null;
        Long rowId;
        try {
            // Get rowId
            nextRowIdSelect.setString(1, tableName);
            rs = nextRowIdSelect.executeQuery();
            rowId = rs.getLong(1);
            // Update next rowId
            incrementRowIdUpdate.setString(1, tableName);
            incrementRowIdUpdate.executeUpdate();
            imdb.commit();
        } catch (SQLException e) {
            logger.error("Error at registering row: {} - {}",
                    e.getMessage(), e.getStackTrace());
            imdb.rollback();
            return null;
        } finally {
            imdb.close(rs);
        }
        // Insert row
        insertRow(rowId, tableName, nodeIds);
        return rowId;
    }

    public synchronized void insertRow(
            Long rowId, String tableName, List<Integer> nodeIds) {
        try {
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
        }
    }

    /**
     * Given a table, it return the ids of the nodes
     * which have data of that table.
     *
     * @param table table name
     * @return list of nodesIds
     */
    public List<Integer> getNodesHaveTable(String table) {
        List<Integer> nodesIDs = new ArrayList<>();
        // Build select
        PreparedStatement st = null;
        String select = "" +
                "SELECT DISTINCT node_id " +
                "FROM tables " +
                "NATURAL JOIN  rows " +
                "WHERE table_name=?;";
        // Execute select
        ResultSet rs = null;
        try {
            st = imdb.prepareStatement(select);
            st.setString(1, table);
            rs = st.executeQuery();
            while (rs.next()) {
                nodesIDs.add(rs.getInt("node_id"));
            }
        } catch (SQLException e) {
            logger.error("Error at getting nodes: {} - {}",
                    e.getMessage(), e.getStackTrace());
            return null; // Table not exist
        } finally {
            imdb.close(rs);
            imdb.close(st);
        }
        return nodesIDs;
    }

    /**
     * Return a map with the tables, and the rows of data of this tables that a node.
     *
     * @param node node
     * @return Map: Table -> List of rows
     */
    public Map<String, List<Long>> getDataNodeHas(RegisteredNode node) {
        logger.debug("Geting tables and rows that node has");
        Map<String, List<Long>> tables = new HashMap<>();
        // Build select
        PreparedStatement st = null;
        String select = "" +
                "SELECT table_name, row_id " +
                "FROM rows " +
                "WHERE node_id=? " +
                "ORDER BY table_name ASC;";
        // Execute select
        ResultSet rs = null;
        try {
            st = imdb.prepareStatement(select);
            st.setLong(1, node.getID());
            rs = st.executeQuery();
            while (rs.next()) {
                String table = rs.getString("table_name");
                if (!tables.containsKey(table)) {
                    tables.put(table, new ArrayList<>());
                }
                tables.get(table).add(rs.getLong("row_id"));
            }
        } catch (SQLException e) {
            logger.error("Error at tables and rows from node: {} - {}",
                    e.getMessage(), e.getStackTrace());
            return null;
        } finally {
            imdb.close(rs);
            imdb.close(st);
        }
        logger.debug(Helper.mapToString(tables));
        return tables;
    }

    public void removeRecordsOfNode(RegisteredNode node) {
        logger.debug("Removing records of node " + node.getID() + " from ImDB of Master.");
        PreparedStatement st = null;
        String delete = "" +
                "DELETE FROM rows " +
                "WHERE node_id=?;";
        // Execute select
        try {
            st = imdb.prepareStatement(delete);
            st.setLong(1, node.getID());
            st.executeUpdate();
            imdb.commit();
        } catch (SQLException e) {
            logger.error("Error at deleting records from node: {} - {}",
                    e.getMessage(), e.getStackTrace());
            imdb.rollback();
        } finally {
            imdb.close(st);
        }
    }

    public void createBackup() {
        imdb.createBackup("backup_imdb.db");
    }

    public void restoreBackup() {
        imdb.restoreBackup("backup_imdb.db");
    }

    public void close() {
        imdb.close(newTableInsert);
        imdb.close(nextRowIdSelect);
        imdb.close(incrementRowIdUpdate);
        imdb.close(newRowInsert);
        imdb.close(selectTableNames);
        imdb.close(selectCreateTableStatements);
        imdb.close();
    }
}
