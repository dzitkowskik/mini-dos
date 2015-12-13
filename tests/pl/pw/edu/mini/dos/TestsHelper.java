package pl.pw.edu.mini.dos;

import pl.pw.edu.mini.dos.communication.ErrorHandler;
import pl.pw.edu.mini.dos.master.Master;
import pl.pw.edu.mini.dos.node.Node;

import java.rmi.RemoteException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestsHelper {
    static String masterIp = "172.17.0.2";
    //static String path = "/home/travis/build/dzitkowskik/mini-dos";
    static String path = "/home/asd/mini-dos2";

    public static void runMasterDemo() {
        new Thread(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        new Master("localhost", 5555);
                    } catch (RemoteException e) {
                        ErrorHandler.handleError(e, false);
                    }
                }
            }
        ).start();
    }

    //

    public static void runNodeDemo() {
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            new Node("localhost", "localhost");
                        } catch (RemoteException e) {
                            ErrorHandler.handleError(e, false);
                        }
                    }
                }
        ).start();
    }

    public static void runMasterDemoInDocker() {
        new Thread(
            new Runnable() {
                @Override
                public void run() {
                try
                {
                    String cmd[] = {"sh", "-c",
                            "docker run -v " + path + ":/dos dos bash -c " +
                                    "\"java -classpath dos/target/ddbms-1.0-SNAPSHOT.jar" +
                                    " pl.pw.edu.mini.dos.master.Master " + masterIp
                                    + " 5555\""};

                    DockerRunner.runCommand(cmd);
                } catch (Throwable t)
                {
                    t.printStackTrace();
                }
                }
            }
        ).start();
    }

    public static void runNodeDemoInDocker(final String nodeIp) {
        new Thread(
            new Runnable() {
                    @Override
                public void run() {
                try
                {
                    String cmd[] = {"sh", "-c",
                            "docker run -v " + path + ":/dos dos bash -c " +
                                    "\"java -classpath dos/target/ddbms-1.0-SNAPSHOT.jar" +
                                    " pl.pw.edu.mini.dos.node.Node " + masterIp
                                    + " " + nodeIp + "\""};

                    DockerRunner.runCommand(cmd);
                } catch (Throwable t)
                {
                    t.printStackTrace();
                }
                }
            }
        ).start();
    }

    public static String getMasterIp() {
        String ip = null;
        try {
            String cmd[] = {"sh", "-c",
                    "docker run dos bash -c \"ifconfig\""};

            List<String> out = DockerRunner.runCommandForResult(cmd);
            Pattern pattern = Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");
            Matcher matcher = pattern.matcher(out.get(1));
            //System.out.println("out[1]:" + out.get(1));
            if (matcher.find()) {
                //System.out.println(matcher.group());
                ip = matcher.group();
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
        return ip;
    }

    public static String getNextIp(String ip) {
        int dot = ip.lastIndexOf(".") + 1;
        int last = Integer.parseInt(ip.substring(dot));
        last++;
        return ip.substring(0, dot) + last;
    }
}
