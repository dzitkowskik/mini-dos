package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.io.Serializable;

public class ExecuteSqlResponse implements Serializable {
    private String result;
    private ErrorEnum error;
    private Object data[];

    public ExecuteSqlResponse() {
    }

    public ExecuteSqlResponse(String result, ErrorEnum error) {
        this.result = result;
        this.error = error;
        this.data = null;
    }

    public ExecuteSqlResponse(String result, ErrorEnum error, Object[] data) {
        this.result = result;
        this.error = error;
        this.data = data;
    }

    public String getResult() {
        return result;
    }

    public Object[] getData() {
        return data;
    }

    public ErrorEnum getError() {
        return error;
    }
}
