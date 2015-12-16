package pl.pw.edu.mini.dos.communication.nodenode;

import java.io.Serializable;

public class ExecuteSqlRequest implements Serializable {
    private Long taskId;
    private String sql;
    private NodeNodeInterface coordinatorNode;

    public ExecuteSqlRequest(Long taskId, String sql, NodeNodeInterface coordinatorNode) {
        this.taskId = taskId;
        this.sql = sql;
        this.coordinatorNode = coordinatorNode;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getSql() {
        return sql;
    }

    public NodeNodeInterface getCoordinatorNode() {
        return coordinatorNode;
    }
}
