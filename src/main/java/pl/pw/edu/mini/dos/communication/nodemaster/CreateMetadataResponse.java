package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

public class CreateMetadataResponse {
    private ErrorEnum error;

    public CreateMetadataResponse(ErrorEnum error) {
        this.error = error;
    }

    public ErrorEnum getError() {
        return error;
    }
}
