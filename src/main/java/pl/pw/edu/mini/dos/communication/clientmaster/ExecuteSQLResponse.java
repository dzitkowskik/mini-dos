package pl.pw.edu.mini.dos.communication.clientmaster;

import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.masternode.ExecuteSQLOnNodeResponse;

import java.io.Serializable;

public class ExecuteSQLResponse implements Serializable {
    private String response;
    private ErrorEnum errorEnum;

    public ExecuteSQLResponse(ExecuteSQLOnNodeResponse response) {
        this.response = response.getResult();
        this.errorEnum = response.getError();
    }

    public String getResponse() {
        return response;
    }
    public ErrorEnum getError() { return errorEnum; }
}
