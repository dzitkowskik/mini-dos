package pl.pw.edu.mini.dos;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.masternode.ExecuteSQLOnNodeRequest;
import pl.pw.edu.mini.dos.communication.masternode.ExecuteSQLOnNodeResponse;
import pl.pw.edu.mini.dos.node.Node;
import pl.pw.edu.mini.dos.node.ndb.SQLStatementVisitor;

import java.rmi.RemoteException;

/**
 * Created by asd on 1/19/16.
 */
public class LateNode extends Node {
    private static final Logger logger = LoggerFactory.getLogger(LateNode.class);
    int delta = 0;

    public LateNode() throws RemoteException {
    }

    public void SetVersionDelta(int delta) {
        this.delta = delta;
    }

    @Override
    public ExecuteSQLOnNodeResponse executeSQLOnNode(ExecuteSQLOnNodeRequest executeSQLOnNodeRequest)
            throws RemoteException {
        Long taskId = executeSQLOnNodeRequest.getTaskId() + delta;
        try {
            Statement stmt = CCJSqlParserUtil.parse(executeSQLOnNodeRequest.getSql());
            SQLStatementVisitor visitor = new SQLStatementVisitor(master, this, taskId);
            stmt.accept(visitor);
            logger.trace("Sending result of query: " + visitor.getResult().getResult());
            return visitor.getResult();
        } catch (JSQLParserException e) {
            logger.error("Sql parsing error: {} - {}", e.getMessage(), e.getStackTrace());
            return new ExecuteSQLOnNodeResponse("", ErrorEnum.SQL_PARSING_ERROR);
        }
    }

}
