package pl.pw.edu.mini.dos.master;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.master.task.TaskManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by asd on 1/19/16.
 *
 * Remove in class:
 *      pl.pw.edu.mini.dos.master.task.TaskManager
 * in method
 *      public synchronized Long newTask(Integer node)
 * 'synchronized' to see different ;)
 */
public class CreatingConcurrentTasksTest {
    private static final Logger logger
            = LoggerFactory.getLogger(CreatingConcurrentTasksTest.class);
    int threadCount = 100;
    int callCount = 300;

    @Test
    public void test() throws Exception {
        final TaskManager taskManager = new TaskManager();
        TestKillingTask[] tasks = new TestKillingTask[threadCount];

        for (int i = 0; i < threadCount; i++) {
            logger.trace("create task #" + i);
            tasks[i] = new TestKillingTask(
                    i * callCount, callCount, taskManager);
            //tasks[i].run();   // sequential
            tasks[i].start();   // parallel
        }

        for (int i = 0; i < threadCount; i++) {
            tasks[i].join();
        }

        String[] lines = taskManager.select().split("\n");
        assertEquals(threadCount * callCount, lines.length);
        logger.trace(taskManager.select());

        boolean[] itWas = new boolean[threadCount * callCount];
        for (int i = 0; i < lines.length; i++) {
            Pattern pattern = Pattern.compile("#(\\d+)");
            Matcher matcher = pattern.matcher(lines[i]);
            assertTrue(matcher.find());
            itWas[Integer.parseInt(matcher.group(1))] = true;
        }

        // try to check something
        for (int i = 0; i < itWas.length; i++) {
            assertTrue(itWas[i]);
        }

        // You can not check id of task, because it can not be predicted
        /*logger.trace(taskManager.select());

        for (int i = 0; i < lines.length; i++) {
            Pattern pattern = Pattern.compile("#(\\d+) \\(n(\\d+)");
            Matcher matcher = pattern.matcher(lines[i]);
            assertTrue(matcher.find());
            assertEquals(matcher.group(2), matcher.group(1));
        } */
    }
}

class TestKillingTask extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(TestKillingTask.class);
    int id, callCount;
    TaskManager taskManager;

    public TestKillingTask(int id, int callCount, TaskManager taskManager) {
        this.id = id;
        this.callCount = callCount;
        this.taskManager = taskManager;
    }

    @Override
    public void run() {
        // wait for rest threads
        try {
            Thread.sleep((1000 - id/callCount)/2);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }

        for (int j = 0; j < callCount; j++) {
            logger.trace("#" + id + " iter #" + j);
            taskManager.newTask(id + j);
        }
    }
}
