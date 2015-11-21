package pl.pw.edu.mini.dos.client;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import pl.pw.edu.mini.dos.communication.Communication;
import pl.pw.edu.mini.dos.communication.clientmaster.ClientMasterInterface;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLRequest;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLResponse;

/**
 * Created by ghash on 21.11.15.
 */
public class ClientRmi {
    private ClientMasterInterface _executor;

    private void setUp() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            Registry registry = LocateRegistry.getRegistry(Communication.RMI_PORT);
            _executor = (ClientMasterInterface) registry.lookup(Communication.RMI_MASTER_ID);
        } catch (Exception e) {
            System.err.println("Compute exception:");
            e.printStackTrace();
        }
    }

    public ExecuteSQLResponse Execute(ExecuteSQLRequest request) throws RemoteException
    {
        return _executor.executeSQL(request);
    }

    public ClientRmi() {
        setUp();
    }
}
