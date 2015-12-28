package pl.pw.edu.mini.dos.master;

import org.junit.Test;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.masternode.*;
import pl.pw.edu.mini.dos.communication.nodemaster.RegisterRequest;
import pl.pw.edu.mini.dos.master.node.RegisteredNode;

import java.rmi.RemoteException;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: asd
 * Date: 12/15/15
 * Time: 8:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class MasterRegisterWithMockitoTest {
    private Master master;
    private MasterNodeInterface node;

    @org.junit.Before
    public void setUp() throws Exception {
        master = new Master();
        node = mock(MasterNodeInterface.class);
    }

    @Test
    public void testRegisterOneNode() throws InterruptedException, RemoteException {
        when(node.checkStatus(any(CheckStatusRequest.class)))
                .thenReturn(new CheckStatusResponse(0, 0, 0));
        when(node.createTables(any(ExecuteCreateTablesRequest.class)))
                .thenReturn(new ExecuteCreateTablesResponse(ErrorEnum.NO_ERROR));

        RegisterRequest registerRequest = new RegisterRequest(node);
        master.register(registerRequest);

        List<RegisteredNode> registeredNodes = master.nodeManager.getNodes();
        assertNotNull(registeredNodes);
        assertEquals(1, registeredNodes.size());
        assertEquals(node, registeredNodes.get(0).getInterface());
    }

}
