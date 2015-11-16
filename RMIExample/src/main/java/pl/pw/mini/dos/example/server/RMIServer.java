package pl.pw.mini.dos.example.server;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.AccessControlException;
import java.security.AllPermission;

/**
 * Created by asd on 11/15/15.
 */
public class RMIServer implements Compute {
    Registry registry = null;
    String name = "Compute";
    String host = "192.168.1.118";
    int registryPort = 1099;

    public RMIServer() {
        setUp(0);   // library choose port
    }

    public RMIServer(int port) {
        super();
        setUp(port);
    }

    private void setUp(int port) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            System.getSecurityManager().checkPermission(new AllPermission());
        } catch (AccessControlException e) {
            System.out.println("client.policy file probably doesn't exist or path is not valid.");
            e.printStackTrace();
            return;
        }


        GetOrCreateRegistry(host, registryPort);

        try {
            Compute engine = this;
            Compute stub =
                    (Compute) UnicastRemoteObject.exportObject(engine, port);
            registry.rebind(name, stub);
            System.out.println("ComputeEngine bound and listening for task...");
        } catch (Exception e) {
            System.err.println("ComputeEngine exception:");
            e.printStackTrace();
        }
    }

    private void GetOrCreateRegistry(String host, int registryPort) {
        try {
            System.out.println("Try get registry...");
            registry = LocateRegistry.getRegistry(host, registryPort);
            System.out.println(registry.list().toString());
            System.out.println("Registry got.");
        } catch (RemoteException e) {
            try {
                System.out.println("Try create registry...");
                registry = LocateRegistry.createRegistry(registryPort);
                System.out.println("Registry created.");
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void close() {
        try {
            registry.unbind(name);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    public <T> T executeTask(Task<T> t) {
        return t.execute();
    }
}

