package pl.pw.edu.mini.dos.node;

import pl.pw.edu.mini.dos.Config;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

/*
 * Created by Karol Dzitkowski on 02.12.2015.
 */

public class Stats {
    private static final Config config = Config.getConfig();

    private static final OperatingSystemMXBean systemMXBean
            =  ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    public long getDbSize() {
        String pathToDBFile = config.getProperty("nodeDatabasePath");
        File file = new File(pathToDBFile);
        return file.length();
    }

    public double getSystemLoad() {
        return systemMXBean.getSystemLoadAverage();
    }

    public long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }
}
