package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.io.Serializable;

public class GetSqlResultResponse implements Serializable {
    private Object result[];
    private ErrorEnum error;

    public GetSqlResultResponse(Object[] result, ErrorEnum error) {
        this.result = result;
        this.error = error;
    }

    public Object[] getResult() {
        return result;
    }

    public ErrorEnum getError() {
        return error;
    }
}
