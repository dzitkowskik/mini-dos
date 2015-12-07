package pl.pw.edu.mini.dos.node.ndb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Config;
import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlRequest;

public class DBmanager {
    private static final Logger logger = LoggerFactory.getLogger(DBmanager.class);
    private static final Config config = Config.getConfig();
    private SQLiteDb db;
    private String pathToDBFile;

    public DBmanager(boolean inMemory) {
        pathToDBFile = "jdbc:sqlite:";
        if(inMemory) {
            pathToDBFile += ":memory:";
        } else {
            pathToDBFile += config.getProperty("nodeDatabasePath");
        }
        this.db = SQLiteDb.getInstance();
    }

    public SQLiteJob newSQLiteJob(ExecuteSqlRequest executeSqlRequest){
        return new SQLiteJob(db.getConnection(pathToDBFile), executeSqlRequest);
    }
}
