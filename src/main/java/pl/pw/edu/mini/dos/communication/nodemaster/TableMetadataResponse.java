package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

public class TableMetadataResponse {
    public ErrorEnum error;

    public TableMetadataResponse(ErrorEnum error) {
        this.error = error;
    }
}
