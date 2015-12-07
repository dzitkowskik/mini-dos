package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.io.Serializable;

public class ExecuteSqlResponse implements Serializable {
    private String result;
    private ErrorEnum error;

    public ExecuteSqlResponse() {
    }

    public ExecuteSqlResponse(String result, ErrorEnum error) {
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
