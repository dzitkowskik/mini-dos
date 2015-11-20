package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.OperationEnum;

public class TableMetadataRequest {
    public OperationEnum operation;
    public String column;
    public String table;

    public TableMetadataRequest(OperationEnum operation, String column, String table) {
        this.operation = operation;
        this.column = column;
        this.table = table;
    }
}
