package pl.pw.edu.mini.dos.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorHandler;
import pl.pw.edu.mini.dos.communication.Services;
import pl.pw.edu.mini.dos.communication.clientmaster.ClientMasterInterface;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLRequest;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLResponse;
import pl.pw.edu.mini.dos.communication.nodemaster.RegisterRequest;

import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class Client {
    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private ClientMasterInterface master;

    public Client(String masterHost, String masterPort, String myIp) throws RemoteException {
        System.setProperty("java.rmi.server.hostname", myIp);   // TODO: It's necessary?

        RMIClient client = new RMIClient(masterHost, Integer.parseInt(masterPort));
        master = (ClientMasterInterface) client.getService(Services.MASTER);
    }

    /**
     * @param args = {"localhost", "1099", "localhost"}
     */
    public static void main(String[] args) throws URISyntaxException, RemoteException {
        Client client = new Client(args[0], args[1], args[2]);

        Scanner scanner = new Scanner(System.in);
        System.out.println("Type the query or enter 'q' to exit:");
        while(scanner.hasNext()) {
            String command = scanner.next();
            logger.debug("Command: " + command);

            if(command.equals("q")) {
                break;
            }

            String result = client.executeSQL(command);
            System.out.println("Result: " + result);
        }

        client.stopClient();
        logger.info("Client stopped!");
    }

    public String executeSQL(String sql){
        ExecuteSQLResponse response;

        try {
            response = master.executeSQL(new ExecuteSQLRequest(sql));
        } catch (RemoteException e) {
            return "Error: " + e.getMessage();
        }
        return response.getResponse();
    }

    public void stopClient() {
        master = null;
        System.exit(0); // Unfortunately, this is only way, to close RMI...
    }
}