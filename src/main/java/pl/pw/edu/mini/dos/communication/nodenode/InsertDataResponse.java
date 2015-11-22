package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.io.Serializable;

public class InsertDataResponse implements Serializable {
    public ErrorEnum error;

    public InsertDataResponse(ErrorEnum error) {
        this.error = error;
    }
}
