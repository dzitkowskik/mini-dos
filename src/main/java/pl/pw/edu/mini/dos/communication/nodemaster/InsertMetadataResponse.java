package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.nodenode.NodeNodeInterface;

import java.io.Serializable;
import java.util.List;

public class InsertMetadataResponse implements Serializable {
    private Long rowId;
    private List<NodeNodeInterface> nodes;
    private ErrorEnum error;

    public InsertMetadataResponse(Long rowId, List<NodeNodeInterface> nodes, ErrorEnum error) {
        this.rowId = rowId;
        this.nodes = nodes;
        this.error = error;
    }

    public Long getRowId() {
        return rowId;
    }

    public List<NodeNodeInterface> getNodes() {
        return nodes;
    }

    public ErrorEnum getError() {
        return error;
    }
}
