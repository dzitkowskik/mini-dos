package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

public class DeleteMetadataResponse {
    public String[] nodes;
    public ErrorEnum error;

    public DeleteMetadataResponse(String[] nodes, ErrorEnum error) {
        this.nodes = nodes;
        this.error = error;
    }
}
