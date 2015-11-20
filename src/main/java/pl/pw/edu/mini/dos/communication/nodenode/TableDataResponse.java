package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

public class TableDataResponse {
    public ErrorEnum error;

    public TableDataResponse(ErrorEnum error) {
        this.error = error;
    }
}
