package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

public class DeleteDataResponse {
    public ErrorEnum error;

    public DeleteDataResponse(ErrorEnum error) {
        this.error = error;
    }
}
