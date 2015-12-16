package pl.pw.edu.mini.dos;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class DemoWithDockerTest {
    @org.junit.Before
    public void setUp() throws Exception {

    }

    @org.junit.After
    public void tearDown() throws Exception {

    }

    @Test
    public void testIsDockerEnvironmentOk() throws InterruptedException {
        DockerRunner docker = DockerRunner.getInstance();
        DockerRunner.DockerThread thread1 = docker.runMasterInDocker("Master");
        Thread.sleep(500);
        DockerRunner.DockerThread thread2 = docker.runNodeInDocker("Node #1");
        DockerRunner.DockerThread thread3 = docker.runNodeInDocker("Node #2");
        DockerRunner.DockerThread thread4 = docker.runClientInDocker("Client");

        for (int i = 0; i < 16; i++) {
            Thread.sleep(1000);
        }

        DockerRunner.getInstance().stopThreads();

        Thread.sleep(1000);

        // when was everything ok, 143 should be
        assertEquals(143, thread1.exitVal);
        assertEquals(143, thread2.exitVal);
        assertEquals(143, thread3.exitVal);
        assertEquals(143, thread4.exitVal);
    }

}
