package pl.pw.edu.mini.dos.master.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.Communication;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.masternode.CheckStatusRequest;
import pl.pw.edu.mini.dos.communication.masternode.CheckStatusResponse;
import pl.pw.edu.mini.dos.communication.masternode.MasterNodeInterface;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Node {
    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(Node.class);

    /**
     * RMI name
     */
    private String name;
    /**
     * IP address
     */
    private String host;
    /**
     * Status
     */
    private StatusNode statusNode;
    /**
     * Executor
     */
    private MasterNodeInterface executor;

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

    /**
     * Connects to the server to the node.
     * @return true if the connection is established successfully, otherwise false
     */
    public ErrorEnum connect() {
        try {
            Registry registry = LocateRegistry.getRegistry(this.host, Communication.RMI_PORT_M_C);
            executor = (MasterNodeInterface) registry.lookup(Communication.RMI_NODE_ID);
        } catch (AccessException e) {
            return ErrorEnum.DENIAL_OF_PERMITS;
        } catch (RemoteException e) {
            return ErrorEnum.REMOTE_EXCEPTION;
        } catch (NotBoundException e) {
            return ErrorEnum.INCORRECT_NAME;
        }
        return ErrorEnum.NO_ERROR;
    }

    /**
     * Checks if the node is up or down. If the node is up, it updates the status figures.
     * @return true if the node is up, otherwise false
     */
    public ErrorEnum checkStatus() {
        try {
            CheckStatusResponse status = executor.checkStatus(new CheckStatusRequest());
            this.statusNode.setStatus(status.getLoadAverage(), status.getCpu(),
                    status.getMemory());
        } catch (RemoteException e) {
            logger.warn("Node down");
            this.statusNode.setDown();
            return ErrorEnum.HOST_IS_UNAVAILABLE;
        }
        return ErrorEnum.NO_ERROR;
    }
}


