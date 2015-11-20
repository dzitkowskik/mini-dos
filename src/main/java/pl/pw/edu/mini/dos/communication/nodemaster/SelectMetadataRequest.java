package pl.pw.edu.mini.dos.communication.nodemaster;

public class SelectMetadataRequest {
    public int rowId;
    public String table;

    public SelectMetadataRequest(int rowId, String table) {
        this.rowId = rowId;
        this.table = table;
    }
}
