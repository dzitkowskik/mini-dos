package pl.pw.edu.mini.dos.communication.masternode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.io.Serializable;

public class ResetDataResponse implements Serializable {
    private ErrorEnum error;

    public ResetDataResponse(ErrorEnum error) {
        this.error = error;
    }

    public ErrorEnum getError() {
        return error;
    }
}
