package pl.pw.edu.mini.dos.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.Communication;
import pl.pw.edu.mini.dos.communication.clientmaster.ClientMasterInterface;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLRequest;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLResponse;

/**
 * Created by ghash on 21.11.15.
 */
public class ClientRmi {
    private static final Logger logger = LoggerFactory.getLogger(ClientRmi.class);
    private ClientMasterInterface executor;

    public ClientRmi() {
        setUp();
    }

    private void setUp() {
//        if (System.getSecurityManager() == null) {
//            System.setSecurityManager(new SecurityManager());
//        }
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", Communication.RMI_PORT_M_C);
            executor = (ClientMasterInterface) registry.lookup(Communication.RMI_MASTER_ID);
        } catch (AccessException e) {
            logger.error("No permissions to call remote method");
            logger.error(e.getStackTrace().toString());
        } catch (RemoteException e) {
            logger.error("Exception while remote method was being executed");
            logger.error(e.getStackTrace().toString());
        } catch (NotBoundException e) {
            logger.error("The name has no associated binding");
            logger.error(e.getStackTrace().toString());
        }
    }

    public ExecuteSQLResponse execute(ExecuteSQLRequest request)
            throws RemoteException {
        return executor.executeSQL(request);
    }
}
