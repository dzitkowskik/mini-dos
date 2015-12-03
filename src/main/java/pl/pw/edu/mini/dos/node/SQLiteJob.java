package pl.pw.edu.mini.dos.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.nodenode.AskToCommitRequest;
import pl.pw.edu.mini.dos.communication.nodenode.AskToCommitResponse;
import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlRequest;
import java.rmi.RemoteException;
import java.sql.SQLException;

/**
 * Created by Karol Dzitkowski on 03.12.2015.
 */
public class SQLiteJob implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Node.class);
    private ExecuteSqlRequest request;

    public SQLiteJob(ExecuteSqlRequest request) {
        this.request = request;
    }

    private class QueryResult {
        public ErrorEnum errorType;
        public String errorMessage;
        public Integer rowsAffected;

        public QueryResult(ErrorEnum errorType, String errorMessage, Integer rowsAffected) {
            this.errorMessage = errorMessage;
            this.errorType = errorType;
            this.rowsAffected = rowsAffected;
        }
    }

    private QueryResult executeQuery(String sql, SqLiteDb db) {
        try {
            Integer rowsAffected = db.ExecuteQuery(request.sql);
            return new QueryResult(ErrorEnum.NO_ERROR, "", rowsAffected);
        } catch (SQLException e) {
            logger.error("Error executing sql query: {} error: {} stack: {}",
                    request.sql,
                    e.getMessage(),
                    e.getStackTrace());
            return new QueryResult(ErrorEnum.SQL_EXECUTION_ERROR, e.getMessage(), 0);
        }
    }

    @Override
    public void run() {
        try (SqLiteDb db = new SqLiteDb()) {
            logger.info("start executing sqlite job");
            QueryResult result = executeQuery(request.sql, db);
            String resultMessage = result.rowsAffected.toString() + " rows affected";

            logger.info(resultMessage);

            // ask if we should commit or rollback and perform suitable action
            try {
                AskToCommitResponse askToCommitResponse =
                    request.node.askToCommit(new AskToCommitRequest(
                        request.taskId, result.errorType, result.errorMessage, resultMessage));

                // COMMIT OR ABORT
                if (askToCommitResponse.commit) {
                    db.commit();
                    logger.info("DB commited");
                } else {
                    db.rollback();
                    logger.info("DB rollbacked");
                }
            } catch (RemoteException e) {
                logger.error("Error while asking to commit: {}", e.getMessage());
            } catch (SQLException e) {
                logger.error("Error while trying commit/rollback: {}", e.getMessage());
            }
        }
    }
}
