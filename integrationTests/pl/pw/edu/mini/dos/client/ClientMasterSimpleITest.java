package pl.pw.edu.mini.dos.client;

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.DockerStuff.DockerRunner;
import pl.pw.edu.mini.dos.DockerStuff.DockerThread;
import pl.pw.edu.mini.dos.TestsHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static pl.pw.edu.mini.dos.TestsHelper.*;

/**
 * Created with IntelliJ IDEA.
 * User: asd
 * Date: 12/17/15
 * Time: 12:13 AM
 * To change this template use File | Settings | File Templates.
 */
public class ClientMasterSimpleITest {
    private static final Logger logger = LoggerFactory.getLogger(ClientMasterSimpleITest.class);

    /**
     * That function is used in testBasicRegisterSetUp test function.
     * It just run Client. It will be run in docker container.
     * @param args
     * Params from command line.
     * DockerRunner give network configuration as command line params on default.
     * Tester can add some params too.
     * @throws Exception
     */
    public void testBasicCommunication_Client(String[] args) throws Exception {
        logger.info("Client started");

        // DockerRunner give network configuration as command line params on default
        // Here we just read it and use to start Client
        Client client = new Client(getMasterIpFromParams(args),
                getMasterPortFromParams(args), getMyIpFromParams(args));

        // if Master is not null (this rather means everything ok)
        assertNotNull(client.master);

        // so we communicated, that's it, finish
        client.stopClient();
        logger.info("Client stopped.");
    }

    /**
     * That function is main test function.
     * It setup instance of application with specified scenario.
     * @throws Exception
     */
    @Test
    public void testBasicRegisterSetUp() throws Exception {
        DockerRunner dockerRunner = DockerRunner.getInstance();

        // run default Master
        dockerRunner.runMasterInDocker("Master");
        TestsHelper.Sleep(5);

        // run test on Client side
        DockerThread clientThread = dockerRunner.runTestInDocker
                (ClientMasterSimpleITest.class, "testBasicCommunication_Client", null, "Client");

        // wait for Client's end
        clientThread.join();

        // if closed correctly
        assertEquals(0, clientThread.exitVal);
    }

    @After
    public void tearDown() throws Exception {
        DockerRunner.getInstance().stopThreads();
    }

}
