package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

public class UpdateDataResponse {
    public ErrorEnum error;

    public UpdateDataResponse(ErrorEnum error) {
        this.error = error;
    }
}
