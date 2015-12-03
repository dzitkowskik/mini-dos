package pl.pw.edu.mini.dos.node;

import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.util.HashMap;

/**
 * Created by Karol Dzitkowski on 03.12.2015.
 */
public class TaskCompletion {

    private class Completion {
        public Integer allTasks;
        public Integer completedTasks = 0;
        public boolean errored = false;
        public ErrorEnum errorType = ErrorEnum.NO_ERROR;
        public String errorMessage = "";
        public String result = "";

        public Completion(Integer allTasks) { this.allTasks = allTasks; }
    }

    private static final Logger logger = LoggerFactory.getLogger(TaskCompletion.class);
    private static TaskCompletion ourInstance = new TaskCompletion();

    public static TaskCompletion getInstance() {
        return ourInstance;
    }

    private HashMap<Integer, Completion> completions;

    private TaskCompletion() {
        completions = new HashMap<>();
    }

    public void add(Integer taskId, Integer tasksNumber) {
        synchronized (completions) {
            completions.put(taskId, new Completion(tasksNumber));
        }
    }

    public void update(
            Integer taskId,
            ErrorEnum error,
            String errorMessage,
            String resultMessage) {
        Completion comp;
        synchronized (completions) {
            comp = completions.get(taskId);
        }
        synchronized (comp) {
            if(error != ErrorEnum.NO_ERROR) {
                comp.errored = true;
                comp.errorMessage = errorMessage;
            }
            comp.errorType = error;
            comp.completedTasks++;
            comp.result = resultMessage;
            comp.notify();
        }
    }

    public void additionalError(Integer taskId, String message) {
        Completion comp;
        synchronized (completions) {
            comp = completions.get(taskId);
        }
        synchronized (comp) {
            comp.errored = true;
            comp.errorType = ErrorEnum.ANOTHER_ERROR;
            comp.errorMessage = message;
            comp.notify();
        }
    }

    public Pair<ErrorEnum, String> waitForCompletion(Integer taskId) {
        Completion comp;
        synchronized (completions) {
             comp = completions.get(taskId);
        }
        synchronized (comp) {
            try {
                while(true) {
                    if (comp.completedTasks == comp.allTasks || comp.errored) {
                        return new Pair<>(
                                comp.errorType,
                                comp.errored ? comp.errorMessage : comp.result);
                    }
                    comp.wait(1000);
                }
            } catch (InterruptedException e) {
                logger.error("Wait interrupted: {}", e.getMessage());
                return new Pair<>(ErrorEnum.ANOTHER_ERROR, e.getMessage());
            }
        }
    }

    public void remove(Integer taskId) {
        synchronized (completions) {
            completions.remove(taskId);
        }
    }
}
