package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.nodenode.NodeNodeInterface;

import java.io.Serializable;
import java.util.List;

public class InsertMetadataResponse implements Serializable {
    private List<NodeNodeInterface> nodes;

    public InsertMetadataResponse(List<NodeNodeInterface> nodes) {
        this.nodes = nodes;
    }

    public List<NodeNodeInterface> getNodes() {
        return nodes;
    }
}
