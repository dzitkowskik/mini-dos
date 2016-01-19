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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    ClientMasterInterface master;

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

        Scanner scanner = new Scanner(System.in);
        System.out.println("+===============================+");
        System.out.println("|          C L I E N T          |");
        System.out.println("+===============================+");
        System.out.println("| Menu:                         |");
        System.out.println("|    'q' -> Quit client         |");
        System.out.println("|    's' -> Insert sample data  |");
        System.out.println("|                               |");
        System.out.println("| Insert SQL query...           |");
        System.out.println("+===============================+");
        System.out.print("ddbms> ");
        label:
        while (scanner.hasNext()) {
            String command = scanner.nextLine();

            switch (command) {
                case "q":
                    scanner.close();
                    break label;
                case "s":
                    for (String c : getSampleData()) {
                        System.out.println(c);
                        System.out.println(client.executeSQL(c));
                    }
                    System.out.print("ddbms> ");
                    break;
                default:
                    while (!command.contains(";")) { // Multiline commands
                        System.out.print("  ...> ");
                        if (scanner.hasNext()) {
                            command += " " + scanner.nextLine();
                        }
                    }
                    logger.debug("Command: " + command);

                    String result = client.executeSQL(command);
                    System.out.println("Result: " + result);
                    System.out.print("ddbms> ");
                    break;
            }
        }

        client.stopClient();
        logger.trace("Client stopped!");
    }

    public static List<String> getSampleData() {
        List<String> sampleData = new ArrayList<>();
        sampleData.add("CREATE TABLE T1(ID1 INTEGER, ID2 INTEGER);");
        sampleData.add("INSERT INTO T1 VALUES(4,1);");
        sampleData.add("INSERT INTO T1 VALUES(3,1);");
        sampleData.add("INSERT INTO T1 VALUES(2,2);");
        sampleData.add("INSERT INTO T1 VALUES(6,2);");
        sampleData.add("CREATE TABLE T2(ID1 INTEGER, ID2 INTEGER);");
        sampleData.add("INSERT INTO T2 (ID1, ID2) VALUES(1,9);");
        sampleData.add("INSERT INTO T2 (ID1, ID2) VALUES(2,10);");
        sampleData.add("SELECT T1.ID1, T1.ID2, T2.ID2 FROM T1 INNER JOIN T2 ON T1.ID2=T2.ID1;");
        return sampleData;
    }

    public String executeSQL(String sql) {
        ExecuteSQLResponse response;

        try {
            response = master.executeSQL(new ExecuteSQLRequest(sql));
        } catch (RemoteException e) {
            return "ERROR - " + e.getMessage();
        }

        if (response.getError() != ErrorEnum.NO_ERROR) {
            return "ERROR - " + response.getError();
        }
        return response.getResponse();
    }

    public void stopClient() {
        master = null;
    }
}