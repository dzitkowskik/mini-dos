package pl.pw.edu.mini.dos.communication.clientmaster;

import java.io.Serializable;

public class ExecuteSQLRequest implements Serializable {
    public String sql;

    public ExecuteSQLRequest(String sql) {
        this.sql = sql;
    }
}
