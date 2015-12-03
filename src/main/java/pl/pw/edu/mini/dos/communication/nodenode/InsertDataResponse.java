package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;
import java.io.Serializable;

public class InsertDataResponse implements Serializable {
    private String response;
    private ErrorEnum error;

    public InsertDataResponse(String response, ErrorEnum error) {
        this.response = response;
        this.error = error;
    }

    public String getResponse() {
        return response;
    }

    public ErrorEnum getError() {
        return error;
    }
}
