package pl.pw.edu.mini.dos.master.rmi;

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

/**
 * Created by asd on 11/15/15.
 */
public class RMIServer {
    Registry registry = null;
    String name = "Compute";
    int registryPort = 1099;

    public RMIServer(String host) {
        System.setProperty("java.rmi.server.hostname", host);

        setPolicyPath();

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        checkPermission();

        GetOrCreateRegistry(host, registryPort);
    }

    private void checkPermission() {
        try {
            System.getSecurityManager().checkPermission(new AllPermission());
        } catch (AccessControlException e) {
            ErrorHandler.handleError(e, true);
        }
    }

    public <T extends Remote> void StartService(String serviceName, int port, T service) {
        try {
            registry.rebind(serviceName, service);
            System.out.println(serviceName + " bound and listening for task...");
        } catch (RemoteException e) {
            ErrorHandler.handleError(e, false);
        }
    }

    private void GetOrCreateRegistry(String host, int registryPort) {
        try {
            System.out.println("Try get registry...");
            registry = LocateRegistry.getRegistry(host, registryPort);
            System.out.println(registry.list().toString());
            System.out.println("Registry got.");
        } catch (Exception e) {
            try {
                System.out.println("Try create registry...");
                registry = LocateRegistry.createRegistry(registryPort);
                System.out.println("Registry created.");
            } catch (RemoteException e1) {
                ErrorHandler.handleError(e1, true);
            }
        }
    }

    private void setPolicyPath() {
        String pathToPolicy = null;
        try {
            pathToPolicy = new File(RMIServer.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getParent() + "/client.policy";
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        System.out.println("Set client.policy to " + pathToPolicy);
        System.setProperty("java.security.policy", pathToPolicy);
    }

    public <T extends Remote> void StopService(String serviceName, T service) {
        try {
            registry.unbind(serviceName);
            UnicastRemoteObject.unexportObject(service, false);
            System.out.println("Service " + serviceName + " stopped.");
        } catch (RemoteException | NotBoundException e) {
            ErrorHandler.handleError(e, false);
        }
    }

}

