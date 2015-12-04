package pl.pw.edu.mini.dos.master.node;

/**
 * Status of a node.
 */
public class StatusNode {
    private boolean running;
    private double loadAverage;
    private long dbSize;
    private long memory;

    public StatusNode() {
        this.running = false;
        this.loadAverage = 0;
        this.dbSize = 0;
        this.memory = 0;
    }

    public void setDown() {
        this.running = false;
    }

    public void setStatus(double loadAverage, long dbSize, long memory) {
        this.running = true;
        this.loadAverage = loadAverage;
        this.dbSize = dbSize;
        this.memory = memory;
    }

    public boolean getStatus() {
        return this.running;
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
