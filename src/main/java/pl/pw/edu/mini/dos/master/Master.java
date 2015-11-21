package pl.pw.edu.mini.dos.master;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.master.rmi.RMIServer;

import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class Master {
    private static final Logger logger = LoggerFactory.getLogger(Master.class);

    public static void main(String[] args) {
        System.setProperty("java.rmi.server.hostname", "localhost");
        System.setProperty("java.security.policy", "/home/ghash/Dokumenty/mini-dos/src/main/resources/client.policy");
        logger.info("Server started!");
        try {
            RMIServer server = new RMIServer();
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();
            server.close();
            logger.info("Server closed!");
        } catch (UnknownHostException e) {
            e.printStackTrace();
            logger.error(e.getMessage().toString());
        } catch (RemoteException e) {
            e.printStackTrace();
            logger.error(e.getMessage().toString());
        }
    }
}
