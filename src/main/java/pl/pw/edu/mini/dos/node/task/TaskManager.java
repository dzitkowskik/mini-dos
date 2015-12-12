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
    private final HashMap<Long, Task> tasks;

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
            tasks.put(taskId, new Task(subTasksNumber));
        }
    }

    /**
     * Update status of a subtask.
     *
     * @param taskId       task id
     * @param subTaskError subtask status
     */
    public void updateSubTask(Long taskId, ErrorEnum subTaskError) {
        Task task;
        synchronized (tasks) {
            task = tasks.get(taskId);
        }
        if (!task.isTaskCompleted() && !subTaskError.equals(ErrorEnum.NO_ERROR)) {
            task.setError(true);
        }
        task.subTaskCompleted();
    }

    /**
     * Wait until all the subtask of the given task are done.
     *
     * @param taskId task id
     * @return true if no error / false if no error
     */
    public boolean waitForCompletion(Long taskId) {
        Task task;
        synchronized (tasks) {
            task = tasks.get(taskId);
        }
        try {
            while (!task.isTaskCompleted()) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            logger.error("Wait interrupted: {}", e.getMessage());
            tasks.remove(taskId);
        }
        boolean error = task.getError();
        tasks.remove(taskId);
        logger.debug("Task {} removed", taskId);
        return error;
    }

    private class Task {
        private int numSubTasks;
        private int completedSubTasks;
        private boolean error;

        public Task(Integer numSubTasks) {
            this.numSubTasks = numSubTasks;
            this.error = false;
            completedSubTasks = 0;
        }

        public boolean isTaskCompleted() {
            return error || numSubTasks == completedSubTasks;
        }

        public boolean getError() {
            return error;
        }

        public void setError(boolean subTaskError) {
            this.error = subTaskError;
        }

        public void subTaskCompleted(){
            completedSubTasks++;
        }
    }
}
