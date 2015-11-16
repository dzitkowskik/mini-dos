package pl.pw.mini.dos.example;

import pl.pw.mini.dos.example.client.CalcTask;
import pl.pw.mini.dos.example.client.CalcTaskResult;
import pl.pw.mini.dos.example.client.RMIClient;
import pl.pw.mini.dos.example.client.SumTask;
import pl.pw.mini.dos.example.server.RMIServer;
import pl.pw.mini.dos.example.server.ServerData;

import java.rmi.RemoteException;

/**
 * Created by asd on 11/15/15.
 */
public class Main {
    public static void main(String[] args) {
        //System.setProperty("java.rmi.server.codebase", "file://home/asd/IdeaProjects/dos/target/dos-1.0-SNAPSHOT.jar");
        System.setProperty("java.rmi.server.hostname", "192.168.190.128");
        System.setProperty("java.security.policy", "/home/asd/IdeaProjects/mini-dos/RMIExample/client.policy");

        if (args[0].equals("s")) {
            ServerData serverData = ServerData.GetInstance();
            serverData.topSecretData = 10;

            RMIServer server = new RMIServer(5000);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Server closing...");
            server.close();
            System.out.println("Good bye!");
        } else if (args[0].equals("c+")) {
            RMIClient client = new RMIClient();
            SumTask task = new SumTask(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            int sum = 0;
            try {
                sum = client.Executor.executeTask(task);
                System.out.println("Result: " + sum);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else if (args[0].equals("cA")) {
            RMIClient client = new RMIClient();
            CalcTask task = new CalcTask(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            CalcTaskResult result;
            try {
                result = client.Executor.executeTask(task);
                System.out.println("Result: " + result);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return;
    }
}
