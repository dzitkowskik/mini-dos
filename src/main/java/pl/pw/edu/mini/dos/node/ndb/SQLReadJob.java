package pl.pw.edu.mini.dos.node.ndb;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Helper;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlRequest;
import pl.pw.edu.mini.dos.communication.nodenode.GetSqlResultResponse;

import java.sql.*;
import java.util.concurrent.Callable;

/**
 * Execute SELECT in a new thread of node.
 */
public class SQLReadJob implements Callable<GetSqlResultResponse> {
    private static final Logger logger = LoggerFactory.getLogger(SQLReadJob.class);
    private Connection conn;
    private ExecuteSqlRequest request;
    private ResultSetHandler<Object[]> handler = null;

    public SQLReadJob(Connection conn, ExecuteSqlRequest request) {
        this.conn = conn;
        this.request = request;
    }

    @Override
    public GetSqlResultResponse call() throws Exception {
        logger.info("Start executing SQLite read job");
        logger.debug("Run: " + request.getSql());

        Object[] result = new String[]{ "" };
        ErrorEnum errorCode = ErrorEnum.NO_ERROR;

        // Execute SQL
        try {
            QueryRunner run = new QueryRunner();
            result = run.query(conn, request.getSql(), getHandler());
        } catch (SQLException e) {
            logger.error("Error({}): {} at {}",
                    e.getErrorCode(),
                    e.getMessage(),
                    request.getSql());
            errorCode = ErrorEnum.SQL_EXECUTION_ERROR;
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("Error({}): {}", e.getErrorCode(), e.getMessage());
                errorCode = ErrorEnum.SQL_EXECUTION_ERROR;
            }
        }
        logger.info("SQLite read job finished");
        logger.debug("Result: " + Helper.arrayToString(result));
        return new GetSqlResultResponse(result, errorCode);
    }

    /**
     * DbUtils. Create a ResultSetHandler implementation to convert a
     * result set into Object[].
     * @return ResultSetHandler
     */
    private ResultSetHandler<Object[]> getHandler() {
        if (handler == null) {
            handler = rs -> {
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
            };
        }
        return handler;
    }
}
