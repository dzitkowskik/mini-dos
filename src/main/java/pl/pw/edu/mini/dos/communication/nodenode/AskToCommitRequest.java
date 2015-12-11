package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.io.Serializable;

public class AskToCommitRequest implements Serializable {
    private Long taskId;
    private ErrorEnum errorType;

    public AskToCommitRequest(Long taskId, ErrorEnum errorType) {
        this.taskId = taskId;
        this.errorType = errorType;
    }

    public Long getTaskId() {
        return taskId;
    }

    public ErrorEnum getErrorType() {
        return errorType;
    }
}
