package pl.pw.edu.mini.dos.communication.masternode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.io.Serializable;

public class ExecuteSQLOnNodeResponse implements Serializable {
    public String result;
    public ErrorEnum error;

    public ExecuteSQLOnNodeResponse() {

    }

    public ExecuteSQLOnNodeResponse(String result, ErrorEnum error) {
        this.result = result;
        this.error = error;
    }

}
