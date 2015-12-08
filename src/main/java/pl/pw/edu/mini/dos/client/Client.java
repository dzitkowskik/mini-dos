package pl.pw.edu.mini.dos.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.Services;
import pl.pw.edu.mini.dos.communication.clientmaster.ClientMasterInterface;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLRequest;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLResponse;

import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    private ClientMasterInterface master;

    public Client() throws RemoteException {
        this("127.0.0.1", "1099", "127.0.0.1");
    }

    public Client(String masterHost, String masterPort, String myIp) throws RemoteException {
        System.setProperty("java.rmi.server.hostname", myIp);   // TODO: It's necessary?

        RMIClient client = new RMIClient(masterHost, Integer.parseInt(masterPort));
        master = (ClientMasterInterface) client.getService(Services.MASTER);
    }

    /**
     * @param args = [masterIpAddress, port, IpAddress]
     */
    public static void main(String[] args) throws URISyntaxException, RemoteException {
        Client client;
        if (args.length == 3) {
            client = new Client(args[0], args[1], args[2]);
        } else {
            client = new Client();
        }

        // for tests {
        String command = "CREATE TABLE DEPARTMENT(ID INT PRIMARY KEY      NOT NULL," +
                " DEPT           CHAR(50) NOT NULL," +
                "   EMP_ID         INT      NOT NULL);";
        System.out.println(command);
        String result = client.executeSQL(command);
        System.out.println("Result: " + result);

        command = "INSERT INTO DEPARTMENT VALUES (1, \"Warsaw Department\", 2);";
        System.out.println(command);
        result = client.executeSQL(command);
        System.out.println("Result: " + result);

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        command = "SELECT * FROM DEPARTMENT;";
        System.out.println(command);
        result = client.executeSQL(command);
        System.out.println("Result: " + result);

        // }

        Scanner scanner = new Scanner(System.in);
        System.out.println("Type the query or enter 'q' to exit:");
        while (scanner.hasNext()) {
            command = scanner.nextLine();
            logger.debug("Command: " + command);

            if (command.equals("q")) {
                scanner.close();
                break;
            }

            result = client.executeSQL(command);
            System.out.println("Result: " + result);
        }

        client.stopClient();
        logger.info("Client stopped!");
    }

    public String executeSQL(String sql) {
        ExecuteSQLResponse response;

        try {
            response = master.executeSQL(new ExecuteSQLRequest(sql));
        } catch (RemoteException e) {
            return "Error: " + e.getMessage();
        }

        if(response.getError() != ErrorEnum.NO_ERROR){
            return "ERROR: " + response.getError() + " \nDetails: " + response.getResponse();
        }
        return response.getResponse();
    }

    public void stopClient() {
        master = null;
        System.exit(0); // Unfortunately, this is only way, to close RMI...
    }
}