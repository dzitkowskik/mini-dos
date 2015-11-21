package pl.pw.edu.mini.dos.master;

import javafx.scene.paint.Material;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.master.rmi.RMIServer;

import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.Scanner;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.master.node.Node;

import java.util.HashMap;
import java.util.Map;

public class Master {
    private static final Logger logger = LoggerFactory.getLogger(Master.class);

    private Map<String, Node> nodes;

    public Master() {
        nodes = new HashMap<String, Node>();
    }

    /**
     * Register  a new node.
     * @param host node's IP
     * @return ErrorEnum
     */
    public ErrorEnum addNode(String host){
        Node newNode = new Node(host);
        // TODO check status
        nodes.put(host, new Node(host));
        return ErrorEnum.SUCCESS;
    }

    public static void main(String[] args) {
        System.setProperty("java.rmi.server.hostname", "localhost");
        System.setProperty("java.security.policy", "/home/ghash/Dokumenty/mini-dos/src/main/resources/client.policy");
        logger.info("Server started!");
        try {
            Master master = new Master();
            RMIServer server = new RMIServer(master);

            System.out.print("*Enter 'q' to stop master.");
            Scanner scanner = new Scanner (System.in);
            while(scanner.hasNext()) {
                String text = scanner.next();
                if(text.equals("q")) {
                    break;
                }
                // operate
            }
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
