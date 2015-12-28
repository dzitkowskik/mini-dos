package pl.pw.edu.mini.dos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.DockerStuff.BashRunner;
import pl.pw.edu.mini.dos.DockerStuff.DockerRunner;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestsHelper {
    private static final Logger logger = LoggerFactory.getLogger(TestsHelper.class);

    public static void Sleep(int seconds) {
        for (int i = 0; i < seconds; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
    }
    public static void Sleep(int seconds, int milliseconds) {
        int delay = seconds * 1000 + milliseconds;

        for (int i = 0; i < delay / 100; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }

        try {
            Thread.sleep(delay%100);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
    }

    public static String generateNetworkParams(DockerRunner dockerRunner) {
        String params = "";
        params += dockerRunner.getMasterIp() + " ";
        params += dockerRunner.getMasterPort() + " ";
        params += dockerRunner.getIP() + " ";
        return params;
    }

    public static String getMasterIpFromParams(String[] params) {
        return params[0];
    }

    public static String getMasterPortFromParams(String[] params) {
        return params[1];
    }

    public static String getMyIpFromParams(String[] params) {
        return params[2];
    }

    public boolean checkCurrentIp(String[] args) {
        String ipExpected = args[0];
        String cmd[] = {"sh", "-c",
                "ifconfig"};

        String ip = "-";
        List<String> out;
        try {
            out = BashRunner.runCommandForResult(cmd);
            while (out.size() == 0) TestsHelper.Sleep(0, 100);

            logger.info("out=" + out);
            Pattern pattern = Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");
            Matcher matcher = pattern.matcher(out.get(1));
            //logger.info("out[1]:" + out.get(1));
            if (matcher.find()) {
                //logger.info(matcher.group());
                ip = matcher.group();
            }
            if (!ipExpected.equals(ip)) {
                logger.error("TestIp fail! (" + ip + " != " + ipExpected + ")." +
                        " Check docker configuration.");
                return false;
            } else {
                logger.info("TestIp passed. (" + ip + " == " + ipExpected + ")");
                return true;
            }
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage());
        }
        return false;
    }

}