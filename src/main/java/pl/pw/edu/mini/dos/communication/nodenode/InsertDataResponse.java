package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;
import java.io.Serializable;

public class InsertDataResponse implements Serializable {
    private ErrorEnum error;
    private String response;

    public InsertDataResponse(ErrorEnum error, String response) {
        this.error = error;
        this.response = response;
    }

    public ErrorEnum getError() {
        return error;
    }
    public String getResponse() { return response; }
}
