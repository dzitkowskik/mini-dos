package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

public class UpdateMetadataResponse {
    public String[] nodes;
    public ErrorEnum error;

    public UpdateMetadataResponse(String[] nodes, ErrorEnum error) {
        this.nodes = nodes;
        this.error = error;
    }
}
