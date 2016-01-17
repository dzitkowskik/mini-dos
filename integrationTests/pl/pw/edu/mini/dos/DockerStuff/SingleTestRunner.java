package pl.pw.edu.mini.dos.DockerStuff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Helper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: asd
 * Date: 12/24/15
 * Time: 4:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class SingleTestRunner {
    private static final Logger logger = LoggerFactory.getLogger(SingleTestRunner.class);

    public static void main(String... args) {
        String[] classAndMethod = args[0].split("#");
        logger.trace("Running " + classAndMethod[0] + "." + classAndMethod[1] + "("
                + Helper.arrayToString(Arrays.copyOfRange(args, 1, args.length)) + ")...");
        try {
            Class c = Class.forName(classAndMethod[0]);
            Method method = c.getMethod(classAndMethod[1], (new String[1]).getClass());
            method.invoke(c.getConstructor(null).newInstance(null),
                    new Object[]{Arrays.copyOfRange(args, 1, args.length)});
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            // asserts handler
            Throwable t = e.getCause();
            if (t != null) {
                logger.error(t.toString());
                t.printStackTrace(new PrintWriter(errors));
                logger.error(errors.toString());
            }

            // exceptions
            e.printStackTrace(new PrintWriter(errors));
            logger.error(errors.toString());

            System.exit(1);
        }
    }

}