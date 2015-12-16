package pl.pw.edu.mini.dos.communication.nodemaster;

import java.io.Serializable;

public class DeleteMetadataRequest implements Serializable {
    private int rowId;
    private String table;

    public DeleteMetadataRequest(int rowId, String table) {
        this.rowId = rowId;
        this.table = table;
    }

    public int getRowId() {
        return rowId;
    }

    public String getTable() {
        return table;
    }
}
