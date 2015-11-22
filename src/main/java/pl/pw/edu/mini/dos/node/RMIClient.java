package pl.pw.edu.mini.dos.node;

import pl.pw.edu.mini.dos.communication.ErrorHandler;

import java.io.File;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.AccessControlException;
import java.security.AllPermission;

public class RMIClient implements Serializable {
    Registry registry;
    int registryPort = 1099;

    public RMIClient(String host) {
        setPolicyPath();

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        checkPermission();

        try {
            registry = LocateRegistry.getRegistry(host, registryPort);
        } catch (RemoteException e) {
            ErrorHandler.handleError(e, true);
        }
    }

    private void checkPermission() {
        try {
            System.getSecurityManager().checkPermission(new AllPermission());
        } catch (AccessControlException e) {
            ErrorHandler.handleError(e, true);
        }
    }

    public Object getService(String serviceName) {
        try {
            return registry.lookup(serviceName);
        } catch (Exception e) {
            ErrorHandler.handleError(e, true);
        }
        return null;
    }

    private void setPolicyPath() {
        String pathToPolicy = null;
        try {
            pathToPolicy = new File(RMIClient.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getParent() + "/client.policy";
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        System.out.println("Set client.policy to " + pathToPolicy);
        System.setProperty("java.security.policy", pathToPolicy);
    }
}
