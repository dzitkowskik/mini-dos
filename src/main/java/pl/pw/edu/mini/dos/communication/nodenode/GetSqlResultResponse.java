package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.io.Serializable;

public class GetSqlResultResponse implements Serializable {
    private String result;
    private SerializableResultSet data;
    private ErrorEnum error;

    public GetSqlResultResponse(String result, SerializableResultSet data, ErrorEnum error) {
        this.result = result;
        this.data = data;
        this.error = error;
    }

    public GetSqlResultResponse(String result, ErrorEnum error) {
        this.result = result;
        this.data = null;
        this.error = error;
    }

    public String getResult() {
        return result;
    }

    public SerializableResultSet getData() {
        return data;
    }

    public ErrorEnum getError() {
        return error;
    }
}
