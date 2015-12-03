package pl.pw.edu.mini.dos.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

/**
 * Created by Karol Dzitkowski on 03.12.2015.
 */
public class WorkQueue
{
    private static final Logger logger = LoggerFactory.getLogger(WorkQueue.class);

    private final int nThreads;
    private final PoolWorker[] threads;
    private final LinkedList queue;

    public WorkQueue(int nThreads)
    {
        this.nThreads = nThreads;
        queue = new LinkedList();
        threads = new PoolWorker[nThreads];

        for (int i=0; i<nThreads; i++) {
            threads[i] = new PoolWorker();
            threads[i].start();
        }
    }

    public void execute(Runnable r) {
        synchronized(queue) {
            queue.addLast(r);
            queue.notify();
        }
    }

    private class PoolWorker extends Thread {
        public void run() {
            Runnable r;
            while (true) {
                synchronized(queue) {
                    while (queue.isEmpty()) {
                        try {
                            queue.wait();
                        } catch (InterruptedException ignored) {}
                    }
                    r = (Runnable) queue.removeFirst();
                }
                // RUN JOB
                try {
                    r.run();
                }
                catch (RuntimeException e) {
                    logger.error("Worker thread runtime exception: {}", e.getMessage());
                }
            }
        }
    }
}
