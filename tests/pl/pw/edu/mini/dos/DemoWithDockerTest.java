package pl.pw.edu.mini.dos;

import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static pl.pw.edu.mini.dos.TestsHelper.getMasterIp;
import static pl.pw.edu.mini.dos.TestsHelper.getNextIp;

public class DemoWithDockerTest {
    @org.junit.Before
    public void setUp() throws Exception {

    }

    @org.junit.After
    public void tearDown() throws Exception {

    }

    @Test
    public void testDemoInDocker() throws InterruptedException {
        String ip = getMasterIp();
        assertNotNull(ip);
        TestsHelper.masterIp = getNextIp(ip);
        String ipNode = getNextIp(ip);

        TestsHelper.runMasterDemoInDocker();
        Thread.sleep(300);
        TestsHelper.runNodeDemoInDocker(ipNode);
        Thread.sleep(300);
        TestsHelper.runNodeDemoInDocker(getNextIp(ipNode));

        Thread.sleep(15000);
    }

}
