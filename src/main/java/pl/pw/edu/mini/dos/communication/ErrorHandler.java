package pl.pw.edu.mini.dos.communication;

import java.io.NotSerializableException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Created by asd on 11/20/15.
 */
public class ErrorHandler {
    public static void handleError(Throwable e, boolean ifExit) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        System.out.println("-----------------------------------------------------------");
        if (e instanceof RemoteException) {
            if (e instanceof ConnectException) {
                //System.out.println(e.getCause().getMessage());
                if (e.getCause().getMessage().equals("Connection refused")) {
                    System.out.println("Connection error. Cannot connect to RMIRegistry. " +
                            "Check if server is running and there are no proxy/antyvir.");
                } else if (e.getCause().getMessage().equals("Connection timed out")) {
                    System.out.println("Failed to create a session because of " +
                            "either an invalid username/password combination, or a firewall problem");
                } else {
                    System.out.println("Unknown error. Check: " +
                            "http://protegewiki.stanford.edu/wiki/Troubleshooting_Client_Server_Connections");
                }
            } else if (cause instanceof NotSerializableException) {
                System.out.println("Add to class " + cause.getMessage() + " ' implements Serializable'.");
            } else {
                System.out.println("Unknown error. Check: " +
                        "http://protegewiki.stanford.edu/wiki/Troubleshooting_Client_Server_Connections");
            }

        } else if (e instanceof NotBoundException) {
            System.out.println("Service not found on server.");
        }
        System.out.println("-----------------------------------------------------------");

        e.printStackTrace();

        if (ifExit) {
            System.exit(1);
        }
    }
}
