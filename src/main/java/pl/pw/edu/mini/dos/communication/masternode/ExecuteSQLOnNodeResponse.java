package pl.pw.edu.mini.dos.communication.masternode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

public class ExecuteSQLOnNodeResponse {
    public String result;
    public ErrorEnum error;

    public ExecuteSQLOnNodeResponse(String result, ErrorEnum error) {
        this.result = result;
        this.error = error;
    }
}
