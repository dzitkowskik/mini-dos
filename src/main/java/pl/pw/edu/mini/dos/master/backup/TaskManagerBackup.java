package pl.pw.edu.mini.dos.master.backup;

import pl.pw.edu.mini.dos.master.task.Task;

import java.io.Serializable;
import java.util.Map;

public class TaskManagerBackup implements Serializable {
    private Map<Long, Task> tasks;
    private Long nextID;

    public TaskManagerBackup(Map<Long, Task> tasks, Long nextID) {
        this.tasks = tasks;
        this.nextID = nextID;
    }

    public Map<Long, Task> getTasks() {
        return tasks;
    }

    public Long getNextID() {
        return nextID;
    }
}
