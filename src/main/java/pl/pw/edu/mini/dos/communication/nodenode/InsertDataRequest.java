package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.Record;

import java.io.Serializable;

public class InsertDataRequest implements Serializable {
    public Record[] data;
    public String table;

    public InsertDataRequest(Record[] data, String table) {
        this.data = data;
        this.table = table;
    }
}
