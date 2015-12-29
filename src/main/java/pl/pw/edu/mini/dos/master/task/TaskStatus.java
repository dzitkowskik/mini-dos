package pl.pw.edu.mini.dos.master.task;

public enum TaskStatus {
    IN_PROCESS("in process"),
    FINISHED("finished"),
    ABORTED("aborted");

    private final String description;

    TaskStatus(String description) {
        this.description = description;
    }

    public boolean equals(TaskStatus e) {
        return this.description == e.toString();
    }

    @Override
    public String toString() {
        return this.description;
    }
}
