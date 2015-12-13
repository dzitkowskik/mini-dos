package pl.pw.edu.mini.dos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class DockerRunner {
    static class StreamGobbler extends Thread
    {
        InputStream is;
        String type;
        boolean isPrint;
        public List<String> out;

        StreamGobbler(InputStream is, String type, boolean isPrint)
        {
            this.is = is;
            this.type = type;
            this.isPrint = isPrint;
            this.out = new LinkedList<>();

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
                        System.out.println(type + ">" + line);
                } else {
                    while ( (line = br.readLine()) != null)
                        out.add(type + ">" + line);
                }
            } catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
        }
    }

    public static void runCommand(String[] cmd) throws IOException, InterruptedException {
        System.out.println("Execing " + Helper.ArrayToString(cmd));
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(cmd);
        // any error message?
        StreamGobbler errorGobbler = new
                StreamGobbler(proc.getErrorStream(), "ERROR", true);

        // any output?
        StreamGobbler outputGobbler = new
                StreamGobbler(proc.getInputStream(), "OUTPUT", true);

        // kick them off
        errorGobbler.start();
        outputGobbler.start();

        // any error???
        int exitVal = proc.waitFor();
        System.out.println("ExitValue: " + exitVal);
        assertEquals(0, exitVal);
    }

    public static List<String> runCommandForResult(String[] cmd) throws IOException, InterruptedException {
        System.out.println("Execing2 " + Helper.ArrayToString(cmd));
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(cmd);
        // any error message?
        StreamGobbler errorGobbler = new
                StreamGobbler(proc.getErrorStream(), "ERROR", true);

        // any output?
        StreamGobbler outputGobbler = new
                StreamGobbler(proc.getInputStream(), "OUTPUT", false);

        // kick them off
        errorGobbler.start();
        outputGobbler.start();

        // any error???
        int exitVal = proc.waitFor();
        System.out.println("ExitValue: " + exitVal);
        assertEquals(0, exitVal);

        return outputGobbler.out;
    }

}
