package pl.pw.edu.mini.dos.communication.nodenode;

import java.io.Serializable;

public class AskToCommitResponse implements Serializable {
    private boolean commit;

    public AskToCommitResponse(boolean commit) {
        this.commit = commit;
    }

    public boolean isCommit() {
        return commit;
    }
}
