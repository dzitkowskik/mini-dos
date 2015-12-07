package pl.pw.edu.mini.dos.node.task;

import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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


    public void add(Long taskId, Integer tasksNumber) {
        synchronized (tasks) {
            tasks.put(taskId, new SubTasks(tasksNumber));
        }
    }

    public void updateSubTask(Long taskId, ErrorEnum subTaskError, String subTaskResult) {
        SubTasks subTasks;
        synchronized (tasks) {
            subTasks = tasks.get(taskId);
        }
        if (!subTasks.isTaskCompleted()) {
            if (!subTaskError.equals(ErrorEnum.NO_ERROR)) {
                subTasks.setError(subTaskError);
            }
            subTasks.addSubTaskResult(subTaskResult);
        }
    }

    public ExecuteSqlResponse waitForCompletion(Long taskId) {
        SubTasks subTasks;
        synchronized (tasks) {
            subTasks = tasks.get(taskId);
        }
        try {
            while (!subTasks.isTaskCompleted()) {
                subTasks.wait(100);
            }
        } catch (InterruptedException e) {
            logger.error("Wait interrupted: {}", e.getMessage());
            tasks.remove(taskId);
            return new ExecuteSqlResponse(e.getMessage(), ErrorEnum.ANOTHER_ERROR);
        }
        ExecuteSqlResponse response =
                new ExecuteSqlResponse(subTasks.getResults().toString(), subTasks.getError());
        tasks.remove(taskId);
        return response;
    }

    private class SubTasks {
        private int subTasks;
        private ErrorEnum errorType;
        private List<String> results;

        public SubTasks(Integer subTasks) {
            this.subTasks = subTasks;
            this.errorType = ErrorEnum.NO_ERROR;
            this.results = new ArrayList<>(subTasks);
        }

        public boolean isTaskCompleted() {
            return errorType != ErrorEnum.NO_ERROR || subTasks == results.size();
        }

        public ErrorEnum getError() {
            return errorType;
        }

        public void setError(ErrorEnum subTaskError) {
            this.errorType = subTaskError;
        }

        public List<String> getResults() {
            return results;
        }

        public void addSubTaskResult(String subTaskResult) {
            this.results.add(subTaskResult);
        }
    }
}
