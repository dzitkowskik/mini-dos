package pl.pw.edu.mini.dos.communication.masternode;

import java.io.Serializable;
import java.util.List;

public class UpdateTablesRequest implements Serializable {
    private List<String> createTableStatements;

    public UpdateTablesRequest(List<String> createTableStatements) {
        this.createTableStatements = createTableStatements;
    }

    public List<String> getCreateTableStatements() {
        return createTableStatements;
    }
}
