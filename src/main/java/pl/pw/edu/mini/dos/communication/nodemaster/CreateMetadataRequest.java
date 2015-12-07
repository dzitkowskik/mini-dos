package pl.pw.edu.mini.dos.communication.nodemaster;

import java.io.Serializable;

public class CreateMetadataRequest implements Serializable {
    private String table;
    private String createStatement;

    public CreateMetadataRequest(String table, String createStatement) {
        this.table = table;
        this.createStatement = createStatement;
    }

    public String getTable() {
        return table;
    }

    public String getCreateStatement() {
        return createStatement;
    }
}
