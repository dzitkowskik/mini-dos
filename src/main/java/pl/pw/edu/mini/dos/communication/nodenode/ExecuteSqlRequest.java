package pl.pw.edu.mini.dos.communication.nodenode;

import java.io.Serializable;

/**
 * Created by Karol Dzitkowski on 03.12.2015.
 */
public class ExecuteSqlRequest implements Serializable {
    public Integer taskId;
    public String sql;
    public NodeNodeInterface node;

    public ExecuteSqlRequest(Integer taskId, String sql, NodeNodeInterface node) {
        this.taskId = taskId;
        this.sql = sql;
        this.node = node;
    }
}
