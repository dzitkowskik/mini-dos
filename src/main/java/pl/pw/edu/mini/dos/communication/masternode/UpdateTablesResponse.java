package pl.pw.edu.mini.dos.communication.masternode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.io.Serializable;

public class UpdateTablesResponse implements Serializable {
    private ErrorEnum error;

    public UpdateTablesResponse(ErrorEnum error) {
        this.error = error;
    }

    public ErrorEnum getError() {
        return error;
    }
}
