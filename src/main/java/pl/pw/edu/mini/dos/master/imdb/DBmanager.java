package pl.pw.edu.mini.dos.master.imdb;

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
    private SQLiteDb imdb;
    // Prepared statements
    PreparedStatement newTableInsert;
    PreparedStatement nextRowIdSelect;
    PreparedStatement incrementRowIdUpdate;
    PreparedStatement newRowInsert;
    PreparedStatement newRowNodeInsert;
    PreparedStatement selectCreateTableStatements;

    public DBmanager() {
        this.imdb = new SQLiteDb();
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
                "row_id		INTEGER PRIMARY KEY	NOT NULL, " +
                "table_name	TEXT				NOT NULL, " +
                "FOREIGN KEY(table_name) REFERENCES tables(table_name)" +
                ")WITHOUT ROWID;";
        String nodesTables = "" +
                "CREATE TABLE nodes(" +
                "node_id INTEGER PRIMARY KEY NOT NULL, " +
                "row_id	 INTEGER			 NOT NULL, " +
                "FOREIGN KEY(row_id) REFERENCES rows(row_id)" +
                ")WITHOUT ROWID;";

        Statement st = null;
        try {
            st = imdb.createStatement();
            st.executeUpdate(tablesTable);
            st.executeUpdate(rowsTable);
            st.executeUpdate(nodesTables);
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
                "INSERT INTO rows (row_id, table_name)" +
                "VALUES (?,?);";
        String newRowNodeInsert = "" +
                "INSERT INTO nodes (node_id, row_id)" +
                "VALUES (?,?);";
        String selectCreateTableStatements = "" +
                "SELECT create_statement FROM tables;";

        try {
            this.newTableInsert = imdb.prepareStatement(newTableInsert);
            this.nextRowIdSelect = imdb.prepareStatement(nextRowIdSelect);
            this.incrementRowIdUpdate = imdb.prepareStatement(incrementRowIdUpdate);
            this.newRowInsert = imdb.prepareStatement(newRowInsert);
            this.newRowNodeInsert = imdb.prepareStatement(newRowNodeInsert);
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
     * @return create statements list
     */
    public List<String> getCreateTableStatements(){
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
     * Insert metadata related with the insert (RowId, TableId, NodesIds).
     * @param tableName tablename
     * @param nodeIds list of nodes ids where data will be insert
     * @return result
     */
    public ErrorEnum insertRow(String tableName, List<Integer> nodeIds){
        ResultSet rs = null;
        try{
            // Get rowId
            nextRowIdSelect.setString(1, tableName);
            rs = nextRowIdSelect.executeQuery();
            Long rowId = rs.getLong(1);
            // Update next rowId
            incrementRowIdUpdate.setString(1, tableName);
            incrementRowIdUpdate.executeUpdate();
            // Insert row
            newRowInsert.setLong(1, rowId);
            newRowInsert.setString(2, tableName);
            newRowInsert.executeUpdate();
            // Insert row nodes
            for (Integer nodeId : nodeIds) {
                newRowNodeInsert.setInt(1,nodeId);
                newRowNodeInsert.setLong(2,rowId);
                newRowNodeInsert.executeUpdate();
            }
            imdb.commit();
        } catch (SQLException e){
            logger.error("Error at registering row: {} - {}",
                    e.getMessage(), e.getStackTrace());
            imdb.rollback();
            return ErrorEnum.TABLE_NOT_EXIST;
        } finally {
            imdb.close(rs);
        }
        return ErrorEnum.NO_ERROR;
    }

    public void close() {
        imdb.close(newTableInsert);
        imdb.close(nextRowIdSelect);
        imdb.close(incrementRowIdUpdate);
        imdb.close(newRowInsert);
        imdb.close(newRowNodeInsert);
        imdb.close(selectCreateTableStatements);
        imdb.close();
    }
}
