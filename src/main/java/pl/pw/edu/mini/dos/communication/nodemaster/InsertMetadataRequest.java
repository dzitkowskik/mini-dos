package pl.pw.edu.mini.dos.communication.nodemaster;

import java.io.Serializable;

public class InsertMetadataRequest implements Serializable {
    public String table;

    public InsertMetadataRequest(String table) {
        this.table = table;
    }
}
