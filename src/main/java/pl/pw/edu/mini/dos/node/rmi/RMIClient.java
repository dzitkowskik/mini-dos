package pl.pw.edu.mini.dos.node.rmi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Helper;
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
    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(RMIClient.class);

    private Registry registry;
    int count = 3;
    int timeout = 3;

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
        pathToPolicy = Helper.getResources(this.getClass(), "client.policy").getFile();
        if (pathToPolicy != null) {
            System.setProperty("java.security.policy", pathToPolicy);
        }
        logger.trace("Set client.policy to " + pathToPolicy);
    }

    private void checkPermission() {
        try {
            System.getSecurityManager().checkPermission(new AllPermission());
        } catch (AccessControlException e) {
            ErrorHandler.handleError(e, true);
        }
    }

    public Remote getService(String serviceName) {
        Exception ee = null;
        for (int i = 0; i < count; i++) {
            try {
                System.out.println("Trying to connect #" + i + "...");
                return registry.lookup(serviceName);
            } catch (Exception e) {
                ee = e;
            }
            try {
                Thread.sleep(timeout * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ErrorHandler.handleError(ee, true);
        return null;
    }
}
