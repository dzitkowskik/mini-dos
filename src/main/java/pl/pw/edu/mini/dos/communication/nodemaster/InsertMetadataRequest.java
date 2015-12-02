package pl.pw.edu.mini.dos.communication.nodemaster;

import java.io.Serializable;

public class InsertMetadataRequest implements Serializable {
    private String table;

    public InsertMetadataRequest(String table) {
        this.table = table;
    }

    public String getTables() {
        return table;
    }
}
