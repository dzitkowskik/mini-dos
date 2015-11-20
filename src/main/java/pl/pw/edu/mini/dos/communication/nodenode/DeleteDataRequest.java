package pl.pw.edu.mini.dos.communication.nodenode;

public class DeleteDataRequest {
    public int rowId;
    public String table;

    public DeleteDataRequest(int rowId, String table) {
        this.rowId = rowId;
        this.table = table;
    }
}
