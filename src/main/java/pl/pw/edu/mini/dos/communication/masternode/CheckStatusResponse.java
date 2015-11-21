package pl.pw.edu.mini.dos.communication.masternode;

import java.io.Serializable;

public class CheckStatusResponse implements Serializable{
    float loadAverage;
    float cpu;
    float memory;

    public CheckStatusResponse(float loadAverage, float cpu, float memory) {
        this.loadAverage = loadAverage;
        this.cpu = cpu;
        this.memory = memory;
    }

    public float getLoadAverage() {
        return loadAverage;
    }

    public float getCpu() {
        return cpu;
    }

    public float getMemory() {
        return memory;
    }
}
