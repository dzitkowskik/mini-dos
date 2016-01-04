package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.nodenode.NodeNodeInterface;

import java.io.Serializable;
import java.util.List;

public class DeleteMetadataResponse implements Serializable {
    private List<NodeNodeInterface> nodes;
    private ErrorEnum error;

    public DeleteMetadataResponse(List<NodeNodeInterface> nodes, ErrorEnum error) {
        this.nodes = nodes;
        this.error = error;
    }

    public List<NodeNodeInterface> getNodes() {
        return nodes;
    }

    public ErrorEnum getError() {
        return error;
    }
}
