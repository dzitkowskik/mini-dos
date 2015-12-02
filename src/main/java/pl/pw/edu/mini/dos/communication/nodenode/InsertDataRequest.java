package pl.pw.edu.mini.dos.communication.nodenode;

import java.io.Serializable;

public class InsertDataRequest implements Serializable {
    private String insertSql;

    public InsertDataRequest(String insertSql) {
        this.insertSql = insertSql;
    }

    public String getInsertSql() {
        return insertSql;
    }
}
