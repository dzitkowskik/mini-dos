package pl.pw.edu.mini.dos.node;

import pl.pw.edu.mini.dos.communication.nodemaster.NodeMasterInterface;
import pl.pw.edu.mini.dos.node.ndb.DBmanager;

/**
 * Created with IntelliJ IDEA.
 * User: asd
 * Date: 1/6/16
 * Time: 1:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class NodeDecapsulation {
    public static NodeMasterInterface getMasterInterface(Node node) {
        return node.master;
    }

    public static DBmanager getDBmanager(Node node) {
        return node.dbManager;
    }
}
