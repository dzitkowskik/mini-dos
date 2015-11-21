package pl.pw.edu.mini.dos.master;

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
        ErrorEnum ok;

        // Create node
        Node newNode = new Node(host);

        // Conect to node
        ok = newNode.connect();
        if(!ok.equals(ErrorEnum.NO_ERROR)){
            return ok;
        }

        // Check status
        ok = newNode.checkStatus();
        if(!ok.equals(ErrorEnum.NO_ERROR)){
            return ok;
        }

        // Add to map of nodes
        synchronized (nodes){
            nodes.put(host, new Node(host));
        }
        return ErrorEnum.NO_ERROR;
    }

    public static void main(String[] args) {
//        System.setProperty("java.rmi.server.hostname", "localhost");
//        System.setProperty("java.security.policy", "/home/ghash/Dokumenty/mini-dos/src/main/resources/client.policy");

        Master master;
        RMIServer server = null;
        Scanner scanner = null;

        logger.info("Server started!");
        System.out.println("*Enter 'q' to stop master.");

        try {
            master = new Master();
            server = new RMIServer(master);
            scanner = new Scanner (System.in);

            while(scanner.hasNext()) {
                String text = scanner.next();
                if(text.equals("q")) {
                    break;
                }
                // operate
            }
        } catch (UnknownHostException e) {
            logger.error(e.getMessage().toString());
            logger.error(e.getStackTrace().toString());
        } catch (RemoteException e) {
            logger.error(e.getMessage().toString());
            logger.error(e.getStackTrace().toString());
        } finally {
            scanner.close();
            server.close();
            logger.info("Server closed!");
        }
    }
}
