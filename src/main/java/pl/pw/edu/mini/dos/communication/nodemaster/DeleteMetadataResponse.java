package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.io.Serializable;

public class DeleteMetadataResponse implements Serializable {
    private String[] nodes;
    private ErrorEnum error;

    public DeleteMetadataResponse(String[] nodes, ErrorEnum error) {
        this.nodes = nodes;
        this.error = error;
    }

    public String[] getNodes() {
        return nodes;
    }

    public ErrorEnum getError() {
        return error;
    }
}
