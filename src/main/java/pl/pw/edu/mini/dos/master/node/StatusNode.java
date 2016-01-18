package pl.pw.edu.mini.dos.master.node;

import java.io.Serializable;

/**
 * Status of a node.
 */
public class StatusNode implements Serializable {
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

    public boolean isDown() {
        return !this.running;
    }

    @Override
    public String toString() {
        String s = "";
        s += isDown() ? "DOWN" : "UP";
        s += " (LA " + loadAverage + ", ";
        s += "DB size " + String.format("%.2f", dbSize / 1024.0) + "KB, ";
        s += "Free Mem. " + String.format("%.2f", memory / (1024.0 * 1024)) + "MB)";
        return s;
    }
}
