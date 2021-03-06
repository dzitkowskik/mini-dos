package pl.pw.edu.mini.dos.communication.nodemaster;

import java.io.Serializable;
import java.util.List;

public class SelectMetadataRequest implements Serializable {
    private List<String> tables;

    public SelectMetadataRequest(List<String> tables) {
        this.tables = tables;
    }

    public List<String> getTables() {
        return tables;
    }
}
