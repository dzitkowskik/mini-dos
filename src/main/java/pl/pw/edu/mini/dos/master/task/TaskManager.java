package pl.pw.edu.mini.dos.master.task;

import java.util.HashMap;
import java.util.Map;

public class TaskManager {
    private Map<Long, Task> tasks;
    private Long nextID;

    public TaskManager() {
        tasks = new HashMap<>();
        this.nextID = 0L;
    }

    /**
     * Create a new task
     *
     * @return taskId
     */
    public synchronized Long newTask(Integer node) {
        Task task = new Task(nextID++, node);
        tasks.put(task.getIdTask(), task);
        return nextID++;
    }
}

