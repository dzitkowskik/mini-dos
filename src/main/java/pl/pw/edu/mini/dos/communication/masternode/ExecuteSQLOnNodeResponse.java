package pl.pw.edu.mini.dos.communication.masternode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.io.Serializable;

public class ExecuteSQLOnNodeResponse implements Serializable {
    private String result;
    private ErrorEnum error;

    public ExecuteSQLOnNodeResponse(String result, ErrorEnum error) {
        this.result = result;
        this.error = error;
    }

    public String getResult() {
        return result;
    }

    public ErrorEnum getError() {
        return error;
    }
}
