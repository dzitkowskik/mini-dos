package pl.pw.edu.mini.dos.master.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.master.backup.TaskManagerBackup;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * It manages all the tasks related with tasks.
 */
public class TaskManager {
    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);
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

    public void createBackup() {
        synchronized (this) {
            TaskManagerBackup backup = new TaskManagerBackup(tasks, nextID);
            try {
                File file = new File("backup_tasks.db");
                FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(backup);
                oos.flush();
                oos.close();
                fos.close();
            } catch (IOException e) {
                logger.error("Unable to backup tasks:" + e.getMessage());
                return;
            }
            logger.trace("Tasks backup created!");
        }
    }

    @SuppressWarnings("unchecked")
    public void restoreBackup() {
        synchronized (this) {
            TaskManagerBackup backup;
            try {
                File file = new File("backup_tasks.db");
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis);
                backup = (TaskManagerBackup) ois.readObject();
                ois.close();
                fis.close();
            } catch (IOException | ClassNotFoundException e) {
                logger.error("Unable to backup tasks. " + e.getMessage());
                return;
            }
            tasks = backup.getTasks();
            nextID = backup.getNextID();
            logger.trace("Tasks backup restored!");
        }
    }
}

