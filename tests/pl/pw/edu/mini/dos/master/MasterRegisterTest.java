package pl.pw.edu.mini.dos.master;

import org.junit.Test;
import pl.pw.edu.mini.dos.communication.nodemaster.RegisterRequest;
import pl.pw.edu.mini.dos.master.node.RegisteredNode;

import java.rmi.RemoteException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: asd
 * Date: 12/15/15
 * Time: 8:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class MasterRegisterTest {
    Master master;
    private MasterNodeRegisterMock node;

    @org.junit.Before
    public void setUp() throws Exception {
        master = new Master();
        node = new MasterNodeRegisterMock();
    }

    @org.junit.After
    public void tearDown() throws Exception {

    }

    @Test
    public void testRegisterOneNode() throws InterruptedException, RemoteException {
        RegisterRequest registerRequest = new RegisterRequest(node);
        master.register(registerRequest);

        List<RegisteredNode> registeredNodes = master.nodeManager.getNodes();
        assertNotNull(registeredNodes);
        assertEquals(1, registeredNodes.size());
        assertEquals(node, registeredNodes.get(0).getInterface());
    }

}
