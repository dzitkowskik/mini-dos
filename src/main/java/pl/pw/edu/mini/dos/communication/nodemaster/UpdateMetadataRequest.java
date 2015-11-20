package pl.pw.edu.mini.dos.communication.nodemaster;

public class UpdateMetadataRequest {
    public int rowId;
    public String table;

    public UpdateMetadataRequest(int rowId, String table) {
        this.rowId = rowId;
        this.table = table;
    }
}
