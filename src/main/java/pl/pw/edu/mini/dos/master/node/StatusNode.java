package pl.pw.edu.mini.dos.master.node;

/**
 * Status of a node.
 */
public class StatusNode {
    private boolean running;
    private float loadAverage;
    private float cpu;
    private float memory;

    public StatusNode(float loadAverage, float cpu, float memory) {
        this.running = true;
        this.loadAverage = loadAverage;
        this.cpu = cpu;
        this.memory = memory;
    }

    public void setDown(){
        this.running = false;
    }

    public void setStatus(float loadAverage, float cpu, float memory) {
        this.running = true;
        this.loadAverage = loadAverage;
        this.cpu = cpu;
        this.memory = memory;
    }

    public boolean getStatus(){
        return this.running;
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
