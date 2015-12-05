package pl.pw.edu.mini.dos.communication.masternode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

public class ExecuteCreateTablesResponse {
    private ErrorEnum error;

    public ExecuteCreateTablesResponse(ErrorEnum error) {
        this.error = error;
    }

    public ErrorEnum getError() {
        return error;
    }
}
