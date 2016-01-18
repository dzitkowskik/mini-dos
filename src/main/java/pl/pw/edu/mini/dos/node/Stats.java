package pl.pw.edu.mini.dos.node;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class Stats {
    private static final OperatingSystemMXBean systemMXBean
            = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    private String dbName;

    public Stats(String dbName) {
        this.dbName = dbName;
    }

    public long getDbSize() {
        File file = new File(dbName);
        return file.length();
    }

    public double getSystemLoad() {
        return systemMXBean.getSystemLoadAverage();
    }

    public long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }
}
