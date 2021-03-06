package pl.pw.edu.mini.dos;

import org.junit.Test;
import pl.pw.edu.mini.dos.DockerStuff.DockerRunner;
import pl.pw.edu.mini.dos.DockerStuff.DockerThread;

import static org.junit.Assert.assertEquals;

public class DemoWithDockerITest {
    @org.junit.Before
    public void setUp() throws Exception {

    }

    @org.junit.After
    public void tearDown() throws Exception {
        DockerRunner.getInstance().stopThreads();
    }

    @Test
    public void testIsDockerEnvironmentOk() throws InterruptedException {
        DockerRunner docker = DockerRunner.getInstance();
        DockerThread thread1 = docker.runMasterInDocker("Master");
        Thread.sleep(500);
        DockerThread thread2 = docker.runNodeInDocker("Node #1");
        DockerThread thread3 = docker.runNodeInDocker("Node #2");
        DockerThread thread4 = docker.runClientInDocker("Client");

        for (int i = 0; i < 16; i++) {
            Thread.sleep(1000);
        }

        DockerRunner.getInstance().stopThreads();

        Thread.sleep(1000);

        // when was everything ok, 143 should be
        assertEquals(137, thread1.exitVal);
        assertEquals(137, thread2.exitVal);
        assertEquals(137, thread3.exitVal);
        assertEquals(137, thread4.exitVal);
    }

}
