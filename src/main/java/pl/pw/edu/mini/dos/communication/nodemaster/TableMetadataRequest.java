package pl.pw.edu.mini.dos.communication.nodemaster;

import java.io.Serializable;

public class TableMetadataRequest implements Serializable {
    private String table;
    private String tableStatement;

    public TableMetadataRequest(String table, String tableStatement) {
        this.table = table;
        this.tableStatement = tableStatement;
    }

    public String getTable() {
        return table;
    }

    public String getTableStatement() {
        return tableStatement;
    }
}
