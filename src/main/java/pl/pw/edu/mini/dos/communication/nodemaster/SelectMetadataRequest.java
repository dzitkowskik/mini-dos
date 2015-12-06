package pl.pw.edu.mini.dos.communication.nodemaster;

import java.util.List;

public class SelectMetadataRequest {
    private int rowId;
    private List<String> tables;

    public SelectMetadataRequest(int rowId, List<String> tables) {
        this.rowId = rowId;
        this.tables = tables;
    }

    public int getRowId() {
        return rowId;
    }

    public List<String> getTables() {
        return tables;
    }
}
