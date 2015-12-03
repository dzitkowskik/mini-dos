package pl.pw.edu.mini.dos.node;

import pl.pw.edu.mini.dos.Config;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Properties;

public final class Stats {
    private static final Properties config = Config.getConfig();
    private static final OperatingSystemMXBean systemMXBean
            = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    private Stats() {
    }

    public static long getDbSize() {
        String pathToDBFile = config.getProperty("nodeDatabasePath");
        File file = new File(pathToDBFile);
        return file.length();
    }

    public static double getSystemLoad() {
        return systemMXBean.getSystemLoadAverage();
    }

    public static long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }
}
