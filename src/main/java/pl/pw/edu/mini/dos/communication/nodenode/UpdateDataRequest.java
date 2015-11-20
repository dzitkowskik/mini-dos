package pl.pw.edu.mini.dos.communication.nodenode;

public class UpdateDataRequest {
    public int rowId;
    public String table;

    public UpdateDataRequest(int rowId, String table) {
        this.rowId = rowId;
        this.table = table;
    }
}
