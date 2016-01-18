package pl.pw.edu.mini.dos.master;

import pl.pw.edu.mini.dos.master.node.NodeManager;

/**
 * Created with IntelliJ IDEA.
 * User: asd
 * Date: 1/6/16
 * Time: 12:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class MasterDecapsulation {
    public static void setNodeManager(Master master, NodeManager nodeManager) {
        master.nodeManager = nodeManager;
    }

    public static Thread getPingThread(Master master) {
        return master.pingThread;
    }

    public static void setPingThread(Master master, Thread pingThread) {
        master.pingThread = pingThread;
    }

}
