package pl.pw.edu.mini.dos.master.rmi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.Communication;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.AccessControlException;
import java.security.AllPermission;

public class RMIServer implements Communication{
    private static final Logger logger = LoggerFactory.getLogger(RMIServer.class);
    Registry registry = null;
    String name;
    String host;
    int registryPort;

    public RMIServer() throws UnknownHostException {
        this.name = Communication.RMI_MASTER_ID;
        this.host =  InetAddress.getLocalHost().getHostAddress();
        this.registryPort = Communication.RMI_PORT;

        setUp(registryPort);
    }

    private void setUp(int port) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            System.getSecurityManager().checkPermission(new AllPermission());
        } catch (AccessControlException e) {
            logger.error("client.policy file probably doesn't exist or path is not valid.");
            logger.error(e.getStackTrace().toString());
            return;
        }

        getOrCreateRegistry(host, port);

        try {
            ClientMaster clientMaster = new ClientMaster();
            NodeMaster nodeMaster = new NodeMaster();

            ClientMaster clientMasterStub = (ClientMaster)
                    UnicastRemoteObject.exportObject(clientMaster, port);
            NodeMaster nodeMasterStub = (NodeMaster)
                    UnicastRemoteObject.exportObject(nodeMaster, port);

            registry.rebind(name, clientMasterStub);
            registry.rebind(name, nodeMasterStub);

            logger.debug("RMIServer running...");
        } catch (Exception e) {
            logger.error("RMIServer exception:");
            logger.error(e.getStackTrace().toString());
        }
    }

    private void getOrCreateRegistry(String host, int registryPort) {
        try {
            logger.trace("Try get registry...");
            registry = LocateRegistry.getRegistry(host, registryPort);
            logger.debug(registry.list().toString());
            logger.trace("Registry got.");
        } catch (RemoteException e) {
            try {
                logger.trace("Try create registry...");
                registry = LocateRegistry.createRegistry(registryPort);
                logger.trace("Registry created.");
            } catch (RemoteException e1) {
                logger.error(e1.getStackTrace().toString());
            }
        }
    }

    public void close() {
        try {
            registry.unbind(name);
        } catch (RemoteException | NotBoundException e) {
            logger.error(e.getStackTrace().toString());
        }
    }

}
