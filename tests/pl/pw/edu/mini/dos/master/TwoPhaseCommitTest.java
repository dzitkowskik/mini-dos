package pl.pw.edu.mini.dos.master;

import org.junit.Test;
import org.mockito.Mock;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLRequest;
import pl.pw.edu.mini.dos.communication.clientmaster.ExecuteSQLResponse;
import pl.pw.edu.mini.dos.communication.nodenode.AskToCommitResponse;
import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlRequest;
import pl.pw.edu.mini.dos.communication.nodenode.GetSqlResultResponse;
import pl.pw.edu.mini.dos.communication.nodenode.NodeNodeInterface;
import pl.pw.edu.mini.dos.master.node.NodeOnMasterDecapsulation;
import pl.pw.edu.mini.dos.master.node.RegisteredNode;
import pl.pw.edu.mini.dos.node.Node;
import pl.pw.edu.mini.dos.node.NodeDecapsulation;

import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * Created by asd on 1/18/16.
 */
public class TwoPhaseCommitTest {
    private Master master;
    private Node node1;
    private Node node2;

    @org.junit.Before
    public void setUp() throws Exception {
        master = new Master();
    }

    @Test
    public void test() throws Exception {
        // partially mock Nodes
        node1 = spy(new Node());
        node2 = spy(new Node());

        // refresh Nodes on Master
        Map<Integer, RegisteredNode> registeredNodes =
                NodeOnMasterDecapsulation.getRegisteredNodes(master.nodeManager);
        registeredNodes.clear();

        RegisteredNode registeredNode = new RegisteredNode(node1);
        registeredNode.setID(0);
        registeredNodes.put(0, registeredNode);

        registeredNode = new RegisteredNode(node2);
        registeredNode.setID(1);
        registeredNodes.put(1, registeredNode);

        // partially mock Node
        doReturn(new AskToCommitResponse(false))
                .when(node2).askToCommit(any());

        // send query which will fail
        String sql = "CREATE TABLE myTable (`Name` varchar(255) default NULL,  " +
                "`Surname` varchar(255) default NULL);";
        ExecuteSQLResponse response = master.executeSQL(new ExecuteSQLRequest(sql));

        // check on Node if table exists
        sql = "SELECT * FROM myTable;";
        Callable<GetSqlResultResponse> callable = NodeDecapsulation.getDBmanager(node2).newSQLJob(
                new ExecuteSqlRequest((long) 99999, sql, node2));
        GetSqlResultResponse response1 = callable.call();

        // not exists.
        assertEquals(ErrorEnum.SQL_EXECUTION_ERROR, response1.getError());
    }

}
