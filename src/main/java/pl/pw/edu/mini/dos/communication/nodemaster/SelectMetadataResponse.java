package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.nodenode.NodeNodeInterface;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class SelectMetadataResponse implements Serializable {
    private List<NodeNodeInterface> nodes;
    private List<String> createTableStatements;
    private ErrorEnum error;

    public SelectMetadataResponse(List<NodeNodeInterface> nodes,
                                  List<String> createTableStatements,
                                  ErrorEnum error) {
        this.nodes = nodes;
        this.createTableStatements = createTableStatements;
        this.error = error;
    }

    public List<NodeNodeInterface> getNodes() {
        return nodes;
    }

    public List<String> getCreateTableStatements() {
        return createTableStatements;
    }

    public ErrorEnum getError() {
        return error;
    }
}
