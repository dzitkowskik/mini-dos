package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.OperationEnum;

public class TableDataRequest {
    public OperationEnum operation;
    public String column;
    public String table;

    public TableDataRequest(OperationEnum operation, String column, String table) {
        this.operation = operation;
        this.column = column;
        this.table = table;
    }
}
