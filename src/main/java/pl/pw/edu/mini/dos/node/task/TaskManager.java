package pl.pw.edu.mini.dos.node.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.util.HashMap;

/**
 * TaskManager manages the status of the tasks.
 * Each task (client's request) is composed in several subtask
 * each one executed in one node.
 */
public class TaskManager {
    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);
    private static TaskManager instance;
    private final HashMap<Long, SubTasks> tasks;

    private TaskManager() {
        tasks = new HashMap<>();
    }

    public static TaskManager getInstance() {
        if (instance == null) {
            instance = new TaskManager();
        }
        return instance;
    }

    /**
     * Add task.
     *
     * @param taskId         task id
     * @param subTasksNumber number of subtasks
     */
    public void add(Long taskId, Integer subTasksNumber) {
        synchronized (tasks) {
            tasks.put(taskId, new SubTasks(subTasksNumber));
        }
    }

    /**
     * Update status of a subtask.
     *
     * @param taskId       task id
     * @param subTaskError subtask status
     */
    public void updateSubTask(Long taskId, ErrorEnum subTaskError) {
        SubTasks subTasks;
        synchronized (tasks) {
            subTasks = tasks.get(taskId);
        }
        if (!subTasks.isTaskCompleted() && !subTaskError.equals(ErrorEnum.NO_ERROR)) {
            subTasks.setError(true);
        }
    }

    /**
     * Wait until all the subtask of the given task are done.
     *
     * @param taskId task id
     */
    public void waitForCompletion(Long taskId) {
        SubTasks subTasks;
        synchronized (tasks) {
            subTasks = tasks.get(taskId);
        }
        try {
            while (!subTasks.isTaskCompleted()) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            logger.error("Wait interrupted: {}", e.getMessage());
            tasks.remove(taskId);
        }
        tasks.remove(taskId);
    }

    private class SubTasks {
        private int subTasks;
        private int completedSubTasks;
        private boolean error;

        public SubTasks(Integer subTasks) {
            this.subTasks = subTasks;
            this.error = false;
            completedSubTasks = 0;
        }

        public boolean isTaskCompleted() {
            return error || subTasks == completedSubTasks;
        }

        public boolean getError() {
            return error;
        }

        public void setError(boolean subTaskError) {
            this.error = subTaskError;
        }
    }
}
