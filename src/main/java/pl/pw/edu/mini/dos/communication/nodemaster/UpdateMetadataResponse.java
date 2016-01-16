package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.nodenode.NodeNodeInterface;

import java.io.Serializable;
import java.util.List;

public class UpdateMetadataResponse implements Serializable {
    private List<NodeNodeInterface> nodes;
    private ErrorEnum error;

    public UpdateMetadataResponse(List<NodeNodeInterface> nodes, ErrorEnum error) {
        this.nodes = nodes;
        this.error = error;
    }

    public ErrorEnum getError() {
        return error;
    }

    public List<NodeNodeInterface> getNodes() {
        return nodes;
    }
}
