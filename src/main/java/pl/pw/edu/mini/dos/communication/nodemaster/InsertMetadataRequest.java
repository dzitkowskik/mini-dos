package pl.pw.edu.mini.dos.communication.nodemaster;

import java.io.Serializable;
import java.util.List;

public class InsertMetadataRequest implements Serializable {
    private List<String> tables;

    public InsertMetadataRequest(List<String> tables) {
        this.tables = tables;
    }

    public List<String> getTables() {
        return tables;
    }
}
