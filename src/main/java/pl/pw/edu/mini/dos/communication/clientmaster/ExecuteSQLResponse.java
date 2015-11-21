package pl.pw.edu.mini.dos.communication.clientmaster;

import java.io.Serializable;

public class ExecuteSQLResponse implements Serializable {
    public String response;

    public ExecuteSQLResponse(String response) {
        this.response = response;
    }
}
