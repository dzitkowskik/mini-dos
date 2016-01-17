package pl.pw.edu.mini.dos.master.task;

import java.io.Serializable;

public class Task implements Serializable {
    private Long idTask;
    private TaskStatus status;
    private Integer node;

    public Task(Long idTask, Integer node) {
        this.idTask = idTask;
        this.status = TaskStatus.IN_PROCESS;
        this.node = node;
    }

    public Long getIdTask() {
        return idTask;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Integer getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }

    @Override
    public String toString() {
        return "#" + idTask + " (n" + node + "): " + status;
    }
}
