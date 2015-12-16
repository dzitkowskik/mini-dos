package pl.pw.edu.mini.dos.communication.nodenode;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutionException;

public interface NodeNodeInterface extends Remote {
    /**
     * Execute sql query on another Node using 2 phase commit.
     * Asks another note to execute sql on his sqlite db
     *
     * @param request Data for executing request (sql and taskId).
     */
    ExecuteSqlResponse executeSql(ExecuteSqlRequest request)
            throws RemoteException;

    /**
     * Return the result of executeSql.
     *
     * @param request taskid
     * @return result of the execution
     */
    GetSqlResultResponse getSqlResult(GetSqlResultRequest request)
            throws RemoteException, ExecutionException, InterruptedException;

    /**
     * Asks node who commissioned a task (executeSql) if it should commit
     * sending him result of his query. IF all nodes involved in this
     * "transaction" succeeded then he will get "COMMIT" in return, else "ABORT"
     * It is needed for implementing two phase commit
     *
     * @param request Contains info about taskId and is query succeeded
     * @return If node should commit or rollback his changes
     */
    AskToCommitResponse askToCommit(AskToCommitRequest request)
            throws RemoteException;


}
