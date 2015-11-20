package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

public class InsertDataResponse {
    public ErrorEnum error;

    public InsertDataResponse(ErrorEnum error) {
        this.error = error;
    }
}
