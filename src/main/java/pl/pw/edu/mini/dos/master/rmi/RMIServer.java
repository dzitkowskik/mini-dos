package pl.pw.edu.mini.dos.master.rmi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorHandler;

import java.io.File;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.AccessControlException;
import java.security.AllPermission;
import java.util.Arrays;

public class RMIServer {
    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(RMIServer.class);

    Registry registry = null;

    public RMIServer(String host, int registryPort) {
        System.setProperty("java.rmi.server.hostname", host);

        setPolicyPath();

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        checkPermission();

        getOrCreateRegistry(host, registryPort);
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

    private void getOrCreateRegistry(String host, int registryPort) {
        try {
            registry = LocateRegistry.getRegistry(host, registryPort);
            logger.info("Registry obtained");
            logger.debug(Arrays.toString(registry.list()));
        } catch (Exception e) {
            try {
                logger.error(e.getMessage());
                registry = LocateRegistry.createRegistry(registryPort);
                logger.info("Registry created");
            } catch (RemoteException e1) {
                ErrorHandler.handleError(e1, true);
            }
        }
    }

    public <T extends Remote> void startService(String serviceName, T service) {
        try {
            registry.rebind(serviceName, service);
            logger.info(serviceName + " bound and listening for task...");
        } catch (RemoteException e) {
            ErrorHandler.handleError(e, false);
        }
    }

    public <T extends Remote> void stopService(String serviceName, T service) {
        try {
            registry.unbind(serviceName);
            UnicastRemoteObject.unexportObject(service, false);
            logger.info("Service " + serviceName + " stopped.");
        } catch (RemoteException | NotBoundException e) {
            ErrorHandler.handleError(e, false);
        }
    }
}