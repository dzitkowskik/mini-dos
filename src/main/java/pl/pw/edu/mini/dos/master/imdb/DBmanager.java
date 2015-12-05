package pl.pw.edu.mini.dos.master.imdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DBmanager {
    private static final Logger logger = LoggerFactory.getLogger(DBmanager.class);
    private IMSqLiteDb imdb;
    // Prepared statements
    PreparedStatement newTableInsert;
    PreparedStatement nextRowIdSelect;
    PreparedStatement incrementRowIdUpdate;
    PreparedStatement newRowInsert;
    PreparedStatement newRowNodeInsert;

    public DBmanager() {
        this.imdb = new IMSqLiteDb();
        prepareStatements();
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
    }

    /**
     * Prepare the Statements that will be used to be faster.
     */
    public void prepareStatements(){
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

        try {
            this.newTableInsert = imdb.prepareStatement(newTableInsert);
            this.nextRowIdSelect = imdb.prepareStatement(nextRowIdSelect);
            this.incrementRowIdUpdate = imdb.prepareStatement(incrementRowIdUpdate);
            this.newRowInsert = imdb.prepareStatement(newRowInsert);
            this.newRowNodeInsert = imdb.prepareStatement(newRowNodeInsert);
        } catch (SQLException e) {
            logger.error("Error at creating prepared statements: {} - {}",
                    e.getMessage(), e.getStackTrace());
            imdb.rollback();
        }
    }

    public ErrorEnum insertTable(String tableName, String createStatement) {
        try{
            newTableInsert.setString(1, tableName);
            newTableInsert.setString(2, createStatement);
            newTableInsert.executeUpdate();
            imdb.commit();
        } catch (SQLException e){
            logger.error("Error at registering table: {} - {}",
                    e.getMessage(), e.getStackTrace());
            imdb.rollback();
            return ErrorEnum.REGISTRING_TABLE_ERROR;
        }
        return ErrorEnum.NO_ERROR;
    }

    public void close() {
        imdb.close(newTableInsert);
        imdb.close(nextRowIdSelect);
        imdb.close(incrementRowIdUpdate);
        imdb.close(newRowInsert);
        imdb.close(newRowNodeInsert);
        imdb.close();
    }
}
