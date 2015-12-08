package pl.pw.edu.mini.dos.node.ndb;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Helper;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.nodenode.AskToCommitRequest;
import pl.pw.edu.mini.dos.communication.nodenode.AskToCommitResponse;
import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlRequest;
import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlResponse;

import java.rmi.RemoteException;
import java.sql.*;

public class SQLiteJob implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SQLiteJob.class);
    private Connection conn;
    private ExecuteSqlRequest request;
    private ExecuteSqlResponse executeSqlResponse;
    private ResultSetHandler<Object[]> handler = null;

    public SQLiteJob(Connection conn, ExecuteSqlRequest request) {
        this.conn = conn;
        this.request = request;
    }

    private ResultSetHandler<Object[]> getHandler() {
        if (handler == null) {
            // create it
            handler = new ResultSetHandler<Object[]>() {
                public Object[] handle(ResultSet rs) throws SQLException {
                    if (!rs.next()) {
                        return null;
                    }

                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    Object[] result = new Object[cols];

                    for (int i = 0; i < cols; i++) {
                        result[i] = rs.getObject(i + 1);
                    }

                    return result;
                }
            };
        }
        return handler;
    }

    public void runReadQuery() {
        ErrorEnum errorCode = ErrorEnum.NO_ERROR;
        Object[] result;
        try {
            QueryRunner run = new QueryRunner();

            logger.info("Run select: " + request.getSql());
            result = run.query(conn, request.getSql(), getHandler());
        } catch (SQLException e) {
            logger.error("Error executing sql query: {} error: {}",
                    request.getSql(),
                    e.getMessage());
            errorCode = ErrorEnum.SQL_EXECUTION_ERROR;
            result = new String[]{ e.getMessage() };
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("Error while closing sqlite connection: {} ", e.getMessage());
            }
        }

        logger.info("Running select finished with result: " + Helper.ArrayToString(result));
        executeSqlResponse = new ExecuteSqlResponse(Helper.ArrayToString(result), errorCode, result);
    }

    public void runWriteQuery() {
        ErrorEnum errorCode = ErrorEnum.NO_ERROR;
        String result;
        PreparedStatement st;
        try {
            st = conn.prepareStatement(request.getSql());
            int rowsAffected = st.executeUpdate();
            result = rowsAffected + " rows affected";
        } catch (SQLException e) {
            logger.error("Error executing sql query: {} error: {}",
                    request.getSql(),
                    e.getMessage());
            errorCode = ErrorEnum.SQL_EXECUTION_ERROR;
            result = e.getMessage();
        }

        // Ask if we should commit or rollback and perform suitable action
        try {
            AskToCommitResponse askToCommitResponse =
                    request.getCoordinatorNode().askToCommit(
                            new AskToCommitRequest(request.getTaskId(), errorCode, result));

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
        } catch (SQLException e) {
            logger.error("Error while trying commit/rollback: {}", e.getMessage());
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("Error while closing sqlite connection: {} ", e.getMessage());
            }
        }
        executeSqlResponse = new ExecuteSqlResponse(result, errorCode);
    }

    @Override
    public void run() {
        logger.info("Start executing sqlite job");
        String sql = request.getSql();

        if (sql.startsWith("SELECT")) {
            logger.info("select");
            runReadQuery();
        } else {
            runWriteQuery();
        }
    }

    public ExecuteSqlResponse getExecuteSqlResponse() {
        return executeSqlResponse;
    }
}
