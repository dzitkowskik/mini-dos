package pl.pw.edu.mini.dos.communication.masternode;

import java.io.Serializable;

public class ExecuteSQLOnNodeRequest implements Serializable {
    private String sql;
    // TODO: Implement task id
    private Integer taskId = 5;

    public ExecuteSQLOnNodeRequest(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }
    public Integer getTaskId() { return taskId; }
}
