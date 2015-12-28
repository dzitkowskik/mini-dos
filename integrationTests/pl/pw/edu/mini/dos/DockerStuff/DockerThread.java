package pl.pw.edu.mini.dos.DockerStuff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: asd
 * Date: 12/26/15
 * Time: 10:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class DockerThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(DockerThread.class);

    String cmd[];
    String machineName;
    public int exitVal = -1;

    public DockerThread(String cmd[], String machineName) {
        this.cmd = cmd;
        this.machineName = machineName;
    }

    @Override
    public void run() {
        try {
            exitVal = BashRunner.runCommand(cmd, machineName);
        } catch (java.lang.ThreadDeath t) {
            logger.debug(machineName + ": Thread killed.");
        } catch (Throwable t) {
            logger.error(t.getMessage());
        }

    }
}