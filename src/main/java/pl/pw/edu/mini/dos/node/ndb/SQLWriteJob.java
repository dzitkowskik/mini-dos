package pl.pw.edu.mini.dos.node.ndb;

import org.apache.commons.dbutils.ResultSetHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.nodenode.AskToCommitRequest;
import pl.pw.edu.mini.dos.communication.nodenode.AskToCommitResponse;
import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlRequest;
import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlResponse;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * Execute CREATE TABLE, INSERT, ... in a new thread of node.
 */
public class SQLWriteJob implements Callable<ExecuteSqlResponse> {
    private static final Logger logger = LoggerFactory.getLogger(SQLWriteJob.class);
    private Connection conn;
    private ExecuteSqlRequest request;

    public SQLWriteJob(Connection conn, ExecuteSqlRequest request) {
        this.conn = conn;
        this.request = request;
    }

    @Override
    public ExecuteSqlResponse call() throws Exception {
        logger.info("Start executing SQLite write job");
        logger.info("Run: " + request.getSql());

        String result = "";
        ErrorEnum errorCode = ErrorEnum.NO_ERROR;

        // Execute SQL
        PreparedStatement st;
        try {
            st = conn.prepareStatement(request.getSql());
            int rowsAffected = st.executeUpdate();
            result = rowsAffected + " rows affected";
        } catch (SQLException e) {
            logger.error("Error({}): {} at {}",
                    e.getErrorCode(),
                    e.getMessage(),
                    request.getSql());
            errorCode = ErrorEnum.SQL_EXECUTION_ERROR;
        }

        // Ask if we should commit or rollback and perform suitable action
        try {
            AskToCommitResponse askToCommitResponse =
                    request.getCoordinatorNode().askToCommit(
                            new AskToCommitRequest(request.getTaskId(), errorCode));

            // Commit or abort
            if (askToCommitResponse.isCommit()) {
                conn.commit();
                logger.info("DB commited");
            } else {
                conn.rollback();
                logger.info("DB rollbacked");
            }
        } catch (RemoteException e) {
            logger.error("Error while asking to commit: {}", e.getMessage());
            errorCode = ErrorEnum.REMOTE_EXCEPTION;
        } catch (SQLException e) {
            logger.error("Error({}): {}", e.getErrorCode(), e.getMessage());
            errorCode = ErrorEnum.SQL_EXECUTION_ERROR;
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("Error({}): {}", e.getErrorCode(), e.getMessage());
                errorCode = ErrorEnum.SQL_EXECUTION_ERROR;
            }
        }
        logger.info("SQLite write job finished.\nResult: " + result);
        return new ExecuteSqlResponse(
                new String[]{ result },
                errorCode);
    }
}
