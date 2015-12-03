package pl.pw.edu.mini.dos.communication.masternode;

import java.io.Serializable;

public class CheckStatusResponse implements Serializable {
    private double loadAverage;
    private long dbSize;
    private long memory;

    public CheckStatusResponse(
            double loadAverage, long dbSize, long memory) {
        this.loadAverage = loadAverage;
        this.dbSize = dbSize;
        this.memory = memory;
    }

    public double getLoadAverage() {
        return loadAverage;
    }

    public long getDbSize() {
        return dbSize;
    }

    public long getMemory() {
        return memory;
    }
}
