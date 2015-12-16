package pl.pw.edu.mini.dos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class design for Docker version 1.8.2, which one is on Travis.
 * On 1.9.* that should not working!!! Look to comment below.
 */
public class DockerRunner {
    static DockerRunner instance = null;

    private static final Logger logger = LoggerFactory.getLogger(DockerRunner.class);

    String path = "/home/travis/build/dzitkowskik/mini-dos";
    //String path = "/home/asd/mini-dos";

    String masterPort = "5555";
    String masterIp = null;
    String nextIp = null;
    String nodeJarPath = "dos/target/ddbms-0.1-Node.jar";
    String masterJarPath = "dos/target/ddbms-0.1-Master.jar";
    String clientJarPath = "dos/target/ddbms-0.1-Client.jar";
    String dockerImageName = "dos";
    public List<Thread> threadList;

    DockerRunner() {
        nextIp = getCurrentIp();
        getNextIp();    // fix for Docker v1.8.2; comment it for v1.9.*
        masterIp = nextIp;
        threadList = new ArrayList<>();
    }

    public static DockerRunner getInstance() {
        if (instance == null) {
            instance = new DockerRunner();
        }
        return instance;
    }

    public DockerThread runMasterInDocker(String machineName) {
        String runJava = "java -jar " + masterJarPath +
                " " + masterIp + " " + masterPort;
        String cmd[] = {"sh", "-c",
                "docker run -t --name " + nextIp
                        + " -v " + path + ":/dos " + dockerImageName
                        + " bash -c " + "\"" + runJava + "\""};
        DockerThread thread = new DockerThread(cmd, machineName);
        thread.start();
        threadList.add(thread);

        return thread;
    }

    public DockerThread runNodeInDocker(String machineName) {
        getNextIp();

        String runJava = "java -jar " + nodeJarPath +
                " " + masterIp + " " + masterPort + " " + nextIp;
        String cmd[] = {"sh", "-c",
                "docker run -t --name " + nextIp
                        + " -v " + path + ":/dos " + dockerImageName
                        + " bash -c " + "\"" + runJava + "\""};
        DockerThread thread = new DockerThread(cmd, machineName);
        thread.start();
        threadList.add(thread);

        return thread;
    }

    public DockerThread runClientInDocker(String machineName) {
        getNextIp();

        String runJava = "java -jar " + clientJarPath +
                " " + masterIp + " " + masterPort + " " + nextIp;
        String cmd[] = {"sh", "-c",
                "docker run -t --name " + nextIp
                        + " -v " + path + ":/dos " + dockerImageName
                        + " bash -c " + "\"" + runJava + "\""};
        DockerThread thread = new DockerThread(cmd, machineName);
        thread.start();
        threadList.add(thread);

        return thread;
    }

    public void waitForThreads() {
        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
    }

    public void stopThreads() {
        String ip = masterIp;
        nextIp = ip;
        for (int i = 0; i < threadList.size(); i++) {
            String cmd[] = {"sh", "-c", "docker stop " + ip};

            try {
                BashRunner.runCommand(cmd, "");
            } catch (IOException | InterruptedException e) {
                logger.error(e.getMessage());
            }
            ip = getNextIp();
        }
    }

    String getCurrentIp() {
        String ip = null;
        try {
            String cmd[] = {"sh", "-c",
                    "docker run dos bash -c \"ifconfig\""};

            List<String> out = BashRunner.runCommandForResult(cmd);
            Pattern pattern = Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");
            Matcher matcher = pattern.matcher(out.get(1));
            //logger.info("out[1]:" + out.get(1));
            if (matcher.find()) {
                //logger.info(matcher.group());
                ip = matcher.group();
            }

        } catch (Throwable t) {
            logger.error(t.getMessage());
        }
        return ip;
    }

    String getNextIp() {
        if (nextIp == null) {
            nextIp = getCurrentIp();
        }

        int dot = nextIp.lastIndexOf(".") + 1;
        int last = Integer.parseInt(nextIp.substring(dot));
        last++;
        nextIp = nextIp.substring(0, dot) + last;

        return nextIp;
    }

    class DockerThread extends Thread {
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
}
