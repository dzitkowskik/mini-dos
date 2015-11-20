package pl.pw.edu.mini.dos.master.node;

/**
 * Status of a node.
 */
public class StatusNode {
    float loadAverage;
    float cpu;
    float memory;

    public StatusNode(float loadAverage, float cpu, float memory) {
        this.loadAverage = loadAverage;
        this.cpu = cpu;
        this.memory = memory;
    }

    public float getLoadAverage() {
        return loadAverage;
    }

    public void setLoadAverage(float loadAverage) {
        this.loadAverage = loadAverage;
    }

    public float getCpu() {
        return cpu;
    }

    public void setCpu(float cpu) {
        this.cpu = cpu;
    }

    public float getMemory() {
        return memory;
    }

    public void setMemory(float memory) {
        this.memory = memory;
    }
}
