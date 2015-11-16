package pl.pw.mini.dos.example.client;

import pl.pw.mini.dos.example.server.Compute;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIClient {
    public Compute Executor;

    private void setUp(int port) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            String name = "Compute";
            Registry registry = LocateRegistry.getRegistry(port);
            Executor = (Compute) registry.lookup(name);
        } catch (Exception e) {
            System.err.println("Compute exception:");
            e.printStackTrace();
        }
    }

    public RMIClient(int port) {
        setUp(port);
    }

    public RMIClient() {
        setUp(0);
    }
}
