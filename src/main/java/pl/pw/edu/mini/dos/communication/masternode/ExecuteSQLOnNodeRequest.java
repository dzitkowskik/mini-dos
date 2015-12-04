package pl.pw.edu.mini.dos.communication.masternode;

import java.io.Serializable;

public class ExecuteSQLOnNodeRequest implements Serializable {
    private String sql;
    private Long taskId;

    public ExecuteSQLOnNodeRequest(Long taskId, String sql) {
        this.taskId = taskId;
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }

    public Long getTaskId() {
        return taskId;
    }
}
