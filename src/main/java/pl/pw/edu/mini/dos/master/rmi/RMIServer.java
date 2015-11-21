package pl.pw.edu.mini.dos.master.rmi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.Communication;
import pl.pw.edu.mini.dos.master.Master;

import java.net.UnknownHostException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RMIServer implements Communication {
    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(RMIServer.class);

    Master master;
    Registry[] registrys;
    String name;
    String host;
    int[] ports;
    ClientMaster clientMaster;
    NodeMaster nodeMaster;


    public RMIServer(Master master) throws RemoteException, UnknownHostException {
        this.master = master;
        this.name = Communication.RMI_MASTER_ID;
        //this.host =  InetAddress.getLocalHost().getHostAddress();
        this.host = "localhost";
        ports = new int[2];
        ports[0] = Communication.RMI_PORT_M_C;
        ports[1] = Communication.RMI_PORT_M_N;

        setUp(ports);
    }

    private void setUp(int[] ports) {
//        if (System.getSecurityManager() == null) {
//            System.setSecurityManager(new SecurityManager());
//        }
//        try {
//            System.getSecurityManager().checkPermission(new AllPermission());
//        } catch (AccessControlException e) {
//            logger.error("client.policy file probably doesn't exist or path is not valid.");
//            logger.error(e.getStackTrace().toString());
//            return;
//        }

        registrys = getOrCreateRegistry(host, ports);

        try {
            this.clientMaster = new ClientMaster(master);
            this.nodeMaster = new NodeMaster(master);

            registrys[0].rebind(name, clientMaster);
            registrys[1].rebind(name, nodeMaster);

            logger.debug("RMIServer running...");
        } catch (Exception e) {
            logger.error("RMIServer exception: {}", e.getMessage());
            logger.error(e.getStackTrace().toString());
        }
    }

    private Registry[] getOrCreateRegistry(String host, int[] ports) {
        Registry[] regs = new Registry[ports.length];
        for(int i = 0; i < ports.length; i++){
            try {
                logger.trace("Try get registry...");
                regs[i] = LocateRegistry.getRegistry(host, ports[i]);
                logger.debug(regs[i].list().toString());
                logger.trace("Registry got.");
            } catch (RemoteException e) {
                try {
                    logger.trace("Try create registry...");
                    regs[i] = LocateRegistry.createRegistry(ports[i]);
                    logger.trace("Registry created.");
                } catch (RemoteException e1) {
                    logger.error(e1.getStackTrace().toString());
                }
            }
        }
        return regs;
    }

    public void close() {
        try {
            UnicastRemoteObject.unexportObject(this.clientMaster,true);
            UnicastRemoteObject.unexportObject(this.nodeMaster,true);
        } catch (NoSuchObjectException e) {
            logger.error(e.getStackTrace().toString());
        }

        for(int i = 0; i < this.registrys.length; i++){
            try {
                registrys[i].unbind(name);
            } catch (RemoteException | NotBoundException e) {
                logger.error(e.getStackTrace().toString());
            }
        }
    }
}
