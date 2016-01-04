package pl.pw.edu.mini.dos.communication.nodemaster;

import java.io.Serializable;

public class DeleteMetadataRequest implements Serializable {
    private String table;

    public DeleteMetadataRequest(String table) {
        this.table = table;
    }

    public String getTable() {
        return table;
    }
}
