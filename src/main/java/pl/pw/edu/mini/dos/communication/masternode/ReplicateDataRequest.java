package pl.pw.edu.mini.dos.communication.masternode;

import pl.pw.edu.mini.dos.communication.nodenode.NodeNodeInterface;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ReplicateDataRequest implements Serializable {
    private Long taskId;
    /**
     * Nodes which have the data. Table -> nodes.
     */
    private Map<String, List<NodeNodeInterface>> tableNodes;
    /**
     * Table -> list of rowsIDs.
     */
    private Map<String, List<Long>> tablesRows;

    private List<String> createTableStatements;

    public ReplicateDataRequest(Long taskId, Map<String, List<NodeNodeInterface>> tableNodes,
                                Map<String, List<Long>> tablesRows, List<String> createTableStatements) {
        this.taskId = taskId;
        this.tableNodes = tableNodes;
        this.tablesRows = tablesRows;
        this.createTableStatements = createTableStatements;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Map<String, List<NodeNodeInterface>> getTableNodes() {
        return tableNodes;
    }

    public Map<String, List<Long>> getTablesRows() {
        return tablesRows;
    }

    public List<String> getCreateTableStatements() {
        return createTableStatements;
    }
}
