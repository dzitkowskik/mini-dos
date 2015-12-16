package pl.pw.edu.mini.dos.communication.nodenode;

import java.io.Serializable;

public class GetSqlResultRequest implements Serializable {
    private Long taskId;

    public GetSqlResultRequest(Long taskId) {
        this.taskId = taskId;
    }

    public Long getTaskId() {
        return taskId;
    }
}
