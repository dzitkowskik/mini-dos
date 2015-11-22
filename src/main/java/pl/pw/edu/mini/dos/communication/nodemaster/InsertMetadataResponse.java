package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.nodenode.NodeNodeInterface;

import java.io.Serializable;

public class InsertMetadataResponse implements Serializable {
    public NodeNodeInterface[] nodes;
    public ErrorEnum error;

    public InsertMetadataResponse() {

    }

    public InsertMetadataResponse(NodeNodeInterface[] nodes, ErrorEnum error) {
        this.nodes = nodes;
        this.error = error;
    }
}
