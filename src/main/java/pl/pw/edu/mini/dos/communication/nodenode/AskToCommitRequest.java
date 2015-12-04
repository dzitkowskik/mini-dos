package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.io.Serializable;

/**
 * Created by Karol Dzitkowski on 03.12.2015.
 */
public class AskToCommitRequest implements Serializable {
    public Long taskId;
    public ErrorEnum errorType;
    public String errorMessage;
    public String queryResult;

    public AskToCommitRequest(Long taskId,
                              ErrorEnum errorType,
                              String errorMessage,
                              String queryResult) {
        this.taskId = taskId;
        this.errorMessage = errorMessage;
        this.errorType = errorType;
        this.queryResult = queryResult;
    }
}
