package pl.pw.edu.mini.dos.communication.masternode;

import java.io.Serializable;
import java.util.List;

public class ResetDataRequest implements Serializable {
    private List<String> tables;
    private List<String> createTableStatements;

    public ResetDataRequest(List<String> tables, List<String> createTableStatements) {
        this.tables = tables;
        this.createTableStatements = createTableStatements;
    }

    public List<String> getTables() {
        return tables;
    }

    public List<String> getCreateTableStatements() {
        return createTableStatements;
    }
}
