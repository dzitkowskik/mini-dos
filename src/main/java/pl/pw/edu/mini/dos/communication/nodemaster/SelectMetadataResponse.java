package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.nodenode.NodeNodeInterface;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class SelectMetadataResponse implements Serializable {
    private Map<String, List<NodeNodeInterface>> tableNodes; // Table -> nodes
    private List<String> createTableStatements;
    private ErrorEnum error;

    public SelectMetadataResponse(Map<String, List<NodeNodeInterface>> tableNodes,
                                  List<String> createTableStatements,
                                  ErrorEnum error) {
        this.tableNodes = tableNodes;
        this.createTableStatements = createTableStatements;
        this.error = error;
    }

    public Map<String, List<NodeNodeInterface>> getTableNodes() {
        return tableNodes;
    }

    public List<String> getCreateTableStatements() {
        return createTableStatements;
    }

    public ErrorEnum getError() {
        return error;
    }
}
