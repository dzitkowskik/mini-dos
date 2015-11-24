package pl.pw.edu.mini.dos.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorHandler;

import java.io.File;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.AccessControlException;
import java.security.AllPermission;

public class RMIClient implements Serializable {
    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(RMIClient.class);

    private Registry registry;

    public RMIClient(String serverHost, int serverPort) {
        setPolicyPath();

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        checkPermission();

        try {
            registry = LocateRegistry.getRegistry(serverHost, serverPort);
        } catch (RemoteException e) {
            ErrorHandler.handleError(e, true);
        }
    }

    private void setPolicyPath() {
        String pathToPolicy = null;
        pathToPolicy = this.getClass().getClassLoader().getResource("client.policy").getFile();
        if (pathToPolicy != null) {
            System.setProperty("java.security.policy", pathToPolicy);
        }
        logger.info("Set client.policy to " + pathToPolicy);
    }

    private void checkPermission() {
        try {
            System.getSecurityManager().checkPermission(new AllPermission());
        } catch (AccessControlException e) {
            ErrorHandler.handleError(e, true);
        }
    }

    public Remote getService(String serviceName) {
        try {
            return registry.lookup(serviceName);
        } catch (Exception e) {
            ErrorHandler.handleError(e, true);
        }
        return null;
    }
}
