package pl.pw.edu.mini.dos.node.ndb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.nodenode.*;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * Execute CREATE TABLE, INSERT, ... in a new thread of node.
 */
public class SQLWriteJob implements Callable<GetSqlResultResponse> {
    private static final Logger logger = LoggerFactory.getLogger(SQLWriteJob.class);
    private Connection conn;
    private ExecuteSqlRequest request;

    public SQLWriteJob(Connection conn, ExecuteSqlRequest request) {
        this.conn = conn;
        this.request = request;
    }

    @Override
    public GetSqlResultResponse call() throws Exception {
        logger.trace("Start executing SQLite write job");
        logger.debug("Run: " + request.getSql());

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
        logger.trace("SQL executed");

        // Ask if we should commit or rollback and perform suitable action
        try {
            AskToCommitResponse askToCommitResponse =
                    request.getCoordinatorNode().askToCommit(
                            new AskToCommitRequest(request.getTaskId(), errorCode));

            // Commit or abort
            if (askToCommitResponse.isCommit()) {
                conn.commit();
                logger.trace("DB commited");
            } else {
                conn.rollback();
                logger.trace("DB rollbacked");
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
        logger.trace("SQLite write job finished");
        logger.debug("Result: " + result);
        return new GetSqlResultResponse(result, errorCode);
    }
}
