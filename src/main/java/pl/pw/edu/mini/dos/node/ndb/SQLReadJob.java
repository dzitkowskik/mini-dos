package pl.pw.edu.mini.dos.node.ndb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlRequest;
import pl.pw.edu.mini.dos.communication.nodenode.GetSqlResultResponse;
import pl.pw.edu.mini.dos.communication.nodenode.SerializableResultSet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Execute SELECT in a new thread of node.
 */
public class SQLReadJob implements Callable<GetSqlResultResponse> {
    private static final Logger logger = LoggerFactory.getLogger(SQLReadJob.class);
    private Connection conn;
    private ExecuteSqlRequest request;

    public SQLReadJob(Connection conn, ExecuteSqlRequest request) {
        this.conn = conn;
        this.request = request;
    }

    @Override
    public GetSqlResultResponse call() throws Exception {
        logger.info("Start executing SQLite read job");
        logger.debug("Run: " + request.getSql());

        String result = "";
        SerializableResultSet rs = null;
        ErrorEnum errorCode = ErrorEnum.NO_ERROR;

        // Execute SQL
        try {
            rs = runSelect(request.getSql());
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
        logger.debug(rs.toString());
        return new GetSqlResultResponse(result, rs, errorCode);
    }

    /**
     * Execute SELECT and returns a pair with a list of the types of each column
     * and a list of arrays, each one corresponds to one row of the obtained result set.
     *
     * @param select select SQL statement
     * @return pair of two lists (types, data)
     */
    public SerializableResultSet runSelect(String select) throws SQLException {
        List<String> columnsTypes = new ArrayList<>();
        List<String> columnsNames = new ArrayList<>();
        List<Object[]> data = new ArrayList<>();
        // Execute select
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = conn.prepareStatement(select);
            rs = st.executeQuery();
            // Save columns types and names
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            for (int c = 1; c <= cols; c++) {
                columnsTypes.add(meta.getColumnTypeName(c));
                columnsNames.add(meta.getColumnName(c));
            }
            // Save data
            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int c = 1; c <= cols; c++) {
                    row[c - 1] = rs.getObject(c);
                }
                data.add(row);
            }
        } finally {
            rs.close();
            st.close();
        }
        return new SerializableResultSet(columnsTypes, columnsNames, data);
    }
}