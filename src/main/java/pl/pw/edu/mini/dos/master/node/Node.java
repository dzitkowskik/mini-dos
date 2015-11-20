package pl.pw.edu.mini.dos.master.node;

import pl.pw.edu.mini.dos.communication.Communication;

public class Node {
    /** RMI name */
    String name;
    /** IP address */
    String host;
    /** Status */
    StatusNode statusNode;

    public Node(String host) {
        this.name = Communication.RMI_NODE_ID;
        this.host = host;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public StatusNode getStatusNode() {
        return statusNode;
    }

    public void setStatusNode(StatusNode statusNode) {
        this.statusNode = statusNode;
    }
}
