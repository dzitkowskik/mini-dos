package pl.pw.edu.mini.dos.communication.nodenode;

import java.io.Serializable;

/**
 * Created by Karol Dzitkowski on 03.12.2015.
 */
public class AskToCommitResponse implements Serializable {
    public boolean commit = false;

    public AskToCommitResponse(boolean commit) {
        this.commit = commit;
    }
}
