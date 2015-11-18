package pl.pw.edu.mini.dos.master;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Master {
    private static final Logger logger = LoggerFactory.getLogger(Master.class);

    public static void main(String[] args) {
        logger.info("Hello world Mr. " + args[0] + "!");
    }
}
