package pl.pw.edu.mini.dos.master;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.masternode.*;

import java.rmi.RemoteException;

/**
 * Created with IntelliJ IDEA.
 * User: asd
 * Date: 12/15/15
 * Time: 8:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class MasterNodeRegisterMock implements MasterNodeInterface {
    private static final Logger logger = LoggerFactory.getLogger(MasterNodeRegisterMock.class);

    @Override
    public ExecuteSQLOnNodeResponse executeSQLOnNode(ExecuteSQLOnNodeRequest executeSQLOnNodeRequest) throws RemoteException {
        logger.trace("executeSQLOnNode running...");
        return new ExecuteSQLOnNodeResponse("String result from test", ErrorEnum.NO_ERROR);
    }

    @Override
    public ExecuteCreateTablesResponse createTables(ExecuteCreateTablesRequest executeCreateTablesRequest) throws RemoteException {
        logger.trace("createTables running...");
        return new ExecuteCreateTablesResponse(ErrorEnum.NO_ERROR);
    }

    @Override
    public CheckStatusResponse checkStatus(CheckStatusRequest checkStatusRequest) throws RemoteException {
        logger.trace("checkStatus running...");
        return new CheckStatusResponse(0, 0, 0);
    }

    @Override
    public KillNodeResponse killNode(KillNodeRequest killNodeRequest) throws RemoteException {
        logger.trace("killNode running...");
        return new KillNodeResponse();
    }

    @Override
    public ReplicateDataResponse replicateData(ReplicateDataRequest replicateDataRequest)
            throws RemoteException {
        logger.trace("replicateData running...");
        return new ReplicateDataResponse(ErrorEnum.NO_ERROR);
    }

    @Override
    public UpdateTablesResponse updateTables(UpdateTablesRequest updateTablesRequest) throws RemoteException {
        logger.trace("updateTables running...");
        return new UpdateTablesResponse(ErrorEnum.NO_ERROR);
    }

    @Override
    public UpdateMasterResponse updateMaster(UpdateMasterRequest updateMasterRequest) throws RemoteException {
        logger.trace("updateMaster running...");
        return new UpdateMasterResponse(ErrorEnum.NO_ERROR);
    }
}
