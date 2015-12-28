package pl.pw.edu.mini.dos.DockerStuff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Helper;
import pl.pw.edu.mini.dos.TestsHelper;
import pl.pw.edu.mini.dos.client.Client;
import pl.pw.edu.mini.dos.master.Master;
import pl.pw.edu.mini.dos.node.Node;

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
    String dockerImageName = "dos";
    public List<Thread> threadList;
    String ipTestClassName = TestsHelper.class.getName();
    String ipTestMethodName = "checkCurrentIp";

    DockerRunner() {
        nextIp = getCurrentIp();
        getNextIp();    // fix for Docker v1.8.2; comment it for v1.9.*
        threadList = new ArrayList<>();
    }

    public static DockerRunner getInstance() {
        if (instance == null) {
            instance = new DockerRunner();
        }
        return instance;
    }

    //String mvnRepoPath = "/home/asd/.m2/repository/";
    String mvnRepoPath = "/home/travis/.m2/repository/";
    String[] libsPath = new String[]{
            "com/github/jsqlparser/jsqlparser/0.9.4/jsqlparser-0.9.4.jar",
            "junit/junit/4.12/junit-4.12.jar",
            "log4j/log4j/1.2.17/log4j-1.2.17.jar",
            "org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar",
            "org/slf4j/slf4j-api/1.7.13/slf4j-api-1.7.13.jar",
            "org/slf4j/slf4j-log4j12/1.7.13/slf4j-log4j12-1.7.13.jar",
            "org/xerial/sqlite-jdbc/3.8.11.2/sqlite-jdbc-3.8.11.2.jar"
    };

    public DockerThread runTestInDocker(Class _class, String methodName,
                                        String[] params, String machineName) {
        getNextIp();

        String runIpTest = "java"
                + " -cp ./dos/target/classes/:./dos/target/test-classes/:"
                + Helper.arrayToStringWithPrefix(libsPath, ":", mvnRepoPath)
                + " " + SingleTestRunner.class.getName() + " "
                + ipTestClassName + "#" + ipTestMethodName
                + " " + nextIp;
        String runJava = "java"
                + " -cp ./dos/target/classes/:./dos/target/test-classes/:"
                + Helper.arrayToStringWithPrefix(libsPath, ":", mvnRepoPath)
                + " " + SingleTestRunner.class.getName() + " "
                + _class.getName() + "#" + methodName
                + " " + TestsHelper.generateNetworkParams(this)
                + " " + Helper.arrayToString(params, " ");
        String cmd[] = {"sh", "-c",
                "docker run -t --name " + nextIp
                        + " -v " + path + ":/dos"
                        + " -v " + mvnRepoPath + ":" + mvnRepoPath
                        + " " + dockerImageName
                        + " bash -c " + "\"" + runIpTest + ";" + runJava + "\""};

        DockerThread thread = new DockerThread(cmd, machineName);
        thread.start();
        threadList.add(thread);

        TestsHelper.Sleep(0, 100);
        return thread;
    }

    public DockerThread runMasterInDocker(String machineName) {
        masterIp = nextIp;

        String runIpTest = "java"
                + " -cp ./dos/target/classes/:./dos/target/test-classes/:"
                + Helper.arrayToStringWithPrefix(libsPath, ":", mvnRepoPath)
                + " " + SingleTestRunner.class.getName() + " "
                + ipTestClassName + "#" + ipTestMethodName
                + " " + nextIp;
        String runJava = "java -cp ./dos/target/classes/:"
                + Helper.arrayToStringWithPrefix(libsPath, ":", mvnRepoPath)
                + " " + Master.class.getName()
                + " " + masterIp + " " + masterPort;

        String cmd[] = {"sh", "-c",
                "docker run -t --name " + nextIp
                        + " -v " + path + ":/dos"
                        + " -v " + mvnRepoPath + ":" + mvnRepoPath
                        + " " + dockerImageName
                        + " bash -c " + "\"" + runIpTest + ";" + runJava + "\""};
        DockerThread thread = new DockerThread(cmd, machineName);
        thread.start();
        threadList.add(thread);

        TestsHelper.Sleep(0, 100);
        return thread;
    }

    public DockerThread runNodeInDocker(String machineName) {
        getNextIp();

        String runIpTest = "java"
                + " -cp ./dos/target/classes/:./dos/target/test-classes/:"
                + Helper.arrayToStringWithPrefix(libsPath, ":", mvnRepoPath)
                + " " + SingleTestRunner.class.getName() + " "
                + ipTestClassName + "#" + ipTestMethodName
                + " " + nextIp;
        String runJava = "java -cp ./dos/target/classes/:"
                + Helper.arrayToStringWithPrefix(libsPath, ":", mvnRepoPath)
                + " " + Node.class.getName()
                + " " + masterIp + " " + masterPort + " " + nextIp;
        String cmd[] = {"sh", "-c",
                "docker run -t --name " + nextIp
                        + " -v " + path + ":/dos"
                        + " -v " + mvnRepoPath + ":" + mvnRepoPath
                        + " " + dockerImageName
                        + " bash -c " + "\"" + runIpTest + ";" + runJava + "\""};
        DockerThread thread = new DockerThread(cmd, machineName);
        thread.start();
        threadList.add(thread);

        TestsHelper.Sleep(0, 100);
        return thread;
    }

    public DockerThread runClientInDocker(String machineName) {
        getNextIp();

        String runIpTest = "java"
                + " -cp ./dos/target/classes/:./dos/target/test-classes/:"
                + Helper.arrayToStringWithPrefix(libsPath, ":", mvnRepoPath)
                + " " + SingleTestRunner.class.getName() + " "
                + ipTestClassName + "#" + ipTestMethodName
                + " " + nextIp;
        String runJava = "java -cp ./dos/target/classes/:"
                + Helper.arrayToStringWithPrefix(libsPath, ":", mvnRepoPath)
                + " " + Client.class.getName()
                + " " + masterIp + " " + masterPort + " " + nextIp;
        String cmd[] = {"sh", "-c",
                "docker run -t --name " + nextIp
                        + " -v " + path + ":/dos"
                        + " -v " + mvnRepoPath + ":" + mvnRepoPath
                        + " " + dockerImageName
                        + " bash -c " + "\"" + runIpTest + ";" + runJava + "\""};
        DockerThread thread = new DockerThread(cmd, machineName);
        thread.start();
        threadList.add(thread);

        TestsHelper.Sleep(0, 100);
        return thread;
    }

    public void waitForThreads() {
        logger.info("Waiting for threads...");
        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
    }

    public void stopThreads() {
        logger.info("Killing all threads...");
        String lastIp = nextIp;

        String ip = masterIp;
        nextIp = ip;
        for (int i = 0; i < threadList.size(); i++) {
            killThread(ip);
            ip = getNextIp();
        }
        threadList.clear();

        nextIp = lastIp;
    }

    public void killThread(String name) {
        String cmd[] = {"sh", "-c", "docker stop " + name};

        try {
            BashRunner.runCommand(cmd, "<killer>");
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage());
        }

        cmd[2] = "docker rm " + name;
        try {
            BashRunner.runCommand(cmd, "<killer>");
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage());
        }
        logger.info("Threads killed. NextIp=" + nextIp);
    }

    String getCurrentIp() {
        String ip = null;
        try {
            String cmd[] = {"sh", "-c",
                    "docker run --name forDosTests_ifconfig"
                            + " dos bash -c \"ifconfig\""};

            List<String> out = BashRunner.runCommandForResult(cmd);
            Pattern pattern = Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");
            Matcher matcher = pattern.matcher(out.get(1));
            //logger.info("out[1]:" + out.get(1));
            if (matcher.find()) {
                //logger.info(matcher.group());
                ip = matcher.group();
                logger.info("Current ip = " + ip);
            }

        } catch (Throwable t) {
            logger.error(t.getMessage());
        }

        killThread("forDosTests_ifconfig");
        return ip;
    }

    String getNextIp() {
        if (nextIp == null) {
            nextIp = getCurrentIp();
            System.out.println(nextIp);
        }

        int dot = nextIp.lastIndexOf(".") + 1;
        int last = Integer.parseInt(nextIp.substring(dot));
        last++;
        nextIp = nextIp.substring(0, dot) + last;

        return nextIp;
    }

    public String getMasterPort() {
        return masterPort;
    }

    public String getMasterIp() {
        return masterIp;
    }

    public String getIP() {
        return nextIp;
    }

}
