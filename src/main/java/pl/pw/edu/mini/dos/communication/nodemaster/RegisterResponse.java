package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.io.Serializable;

public class RegisterResponse implements Serializable {
    public ErrorEnum error;

    public RegisterResponse(ErrorEnum error) {
        this.error = error;
    }
}
