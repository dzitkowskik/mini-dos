package pl.pw.edu.mini.dos.master.task;

import java.util.HashMap;
import java.util.Map;

/**
 * It manages all the tasks related with tasks.
 */
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
        Task task = new Task(nextID, node);
        tasks.put(task.getIdTask(), task);
        return nextID++;
    }

    public void setFinishedTask(Long taskID) {
        Task task = tasks.get(taskID);
        task.setStatus(TaskStatus.FINISHED);
    }

    public void setAbortedTask(Long taskID) {
        Task task = tasks.get(taskID);
        task.setStatus(TaskStatus.ABORTED);
    }

    /**
     * @return all tasks with their information
     */
    public String select() {
        String result = "";
        for (Task task : tasks.values()) {
            result += task.toString() + "\n";
        }
        return result;
    }

    /**
     * @param taskID task id
     * @return information of given task
     */
    public String select(Long taskID) {
        return tasks.get(taskID).toString() + "\n";
    }
}

