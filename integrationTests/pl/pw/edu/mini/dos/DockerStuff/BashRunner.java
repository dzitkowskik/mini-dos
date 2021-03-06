package pl.pw.edu.mini.dos.DockerStuff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Helper;
import pl.pw.edu.mini.dos.Utils.TestsHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BashRunner {
    private static final Logger logger = LoggerFactory.getLogger(BashRunner.class);

    static class StreamGobbler extends Thread
    {
        InputStream is;
        String type;
        String prefix;
        boolean isPrint;
        public List<String> out;
        SimpleDateFormat time_formatter = new SimpleDateFormat("HH:mm:ss.SSS");

        private static final Logger logger = LoggerFactory.getLogger(StreamGobbler.class);

        StreamGobbler(InputStream is, String prefix, String type, boolean isPrint)
        {
            this.is = is;
            this.prefix = prefix;
            this.type = type;
            this.isPrint = isPrint;
            this.out = new LinkedList<>();
        }

        String format(String prefix, String line) {
            return String.format("%s, %s: %s",
                    time_formatter.format(System.currentTimeMillis()),
                    prefix, line);
        }

        public void run()
        {
            try
            {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                if (isPrint) {
                    while ( (line = br.readLine()) != null)
                        System.out.println(format(prefix, line));
                } else {
                    while ( (line = br.readLine()) != null)
                        out.add(format(prefix, line));
                }
            } catch (IOException ioe)
            {
                logger.error(ioe.getMessage());
            }
        }
    }

    public static int runCommand(String[] cmd, String prefix) throws IOException, InterruptedException {
        logger.trace("Executing " + Helper.arrayToString(cmd));
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(cmd);
        // any error message?
        StreamGobbler errorGobbler = new
                StreamGobbler(proc.getErrorStream(), prefix, "ERROR", true);

        // any output?
        StreamGobbler outputGobbler = new
                StreamGobbler(proc.getInputStream(), prefix, "OUTPUT", true);

        // kick them off
        errorGobbler.start();
        outputGobbler.start();

        // any error???
        int exitVal = proc.waitFor();
        TestsHelper.Sleep(0, 100);
        //logger.trace("ExitValue: " + exitVal);
        return exitVal;
    }

    public static List<String> runCommandForResult(String[] cmd) throws IOException, InterruptedException {
        logger.trace("Executing2 " + Helper.arrayToString(cmd));
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(cmd);
        // any error message?
        StreamGobbler errorGobbler = new
                StreamGobbler(proc.getErrorStream(), "", "ERROR", true);

        // any output?
        StreamGobbler outputGobbler = new
                StreamGobbler(proc.getInputStream(), "", "OUTPUT", false);

        // kick them off
        errorGobbler.start();
        outputGobbler.start();

        // any error???
        int exitVal = proc.waitFor();
        //logger.trace("ExitValue: " + exitVal);
        assertEquals(0, exitVal);

        return outputGobbler.out;
    }

}
