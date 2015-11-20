package pl.pw.edu.mini.dos.communication.nodemaster;

public class InsertMetadataRequest {
    public int[] rowId;
    public String table;

    public InsertMetadataRequest(int[] rowId, String table) {
        this.rowId = rowId;
        this.table = table;
    }
}
