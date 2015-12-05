package pl.pw.edu.mini.dos.communication.masternode;

import java.util.List;

public class ExecuteCreateTablesRequest {
    // List of create stataments of all the tables
    private List<String> createTableStatements;

    public ExecuteCreateTablesRequest(List<String> createTableStatements) {
        this.createTableStatements = createTableStatements;
    }

    public List<String> getCreateTableStatements() {
        return createTableStatements;
    }
}
