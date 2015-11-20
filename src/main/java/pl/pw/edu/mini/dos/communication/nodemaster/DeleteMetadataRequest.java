package pl.pw.edu.mini.dos.communication.nodemaster;

public class DeleteMetadataRequest {
    public int rowId;
    public String table;

    public DeleteMetadataRequest(int rowId, String table) {
        this.rowId = rowId;
        this.table = table;
    }
}
