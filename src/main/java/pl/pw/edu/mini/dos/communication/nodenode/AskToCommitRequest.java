package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.io.Serializable;

public class AskToCommitRequest implements Serializable {
    private Long taskId;
    private ErrorEnum errorType;
    private String result; // The query result or the error message

    public AskToCommitRequest(Long taskId, ErrorEnum errorType, String result) {
        this.taskId = taskId;
        this.errorType = errorType;
        this.result = result;
    }

    public Long getTaskId() {
        return taskId;
    }

    public ErrorEnum getErrorType() {
        return errorType;
    }

    public String getResult() {
        return result;
    }
}
