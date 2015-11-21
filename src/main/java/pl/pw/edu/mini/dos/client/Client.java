package pl.pw.edu.mini.dos.client;

/**
 * Created by Karol Dzitkowski on 20.11.15.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLRequest;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLResponse;

import java.rmi.RemoteException;
import java.util.Scanner;

public class Client {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
//        System.setProperty("java.rmi.server.hostname", "localhost");
//        System.setProperty("java.security.policy", "/home/ghash/Dokumenty/mini-dos/src/main/resources/client.policy");

        logger.info("Client started!");

        ClientRmi client = new ClientRmi();

        Scanner scanner = new Scanner (System.in);
        System.out.println("Type the query or enter 'q' to exit:");

        while(scanner.hasNext()) {
            String command = scanner.next();
            logger.debug("Command: " + command);

            if(command.equals("q")) {
                break;
            }

            try {
                ExecuteSQLResponse response = client.Execute(new ExecuteSQLRequest(command));
                logger.debug("Response: " + response.getResponse());
            } catch (RemoteException e) {
                logger.error("Exception while remote method was being executed");
                logger.error(e.getStackTrace().toString());
            }
        }
        logger.info("Client quit");
    }
}