package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.Record;

import java.io.Serializable;
import java.util.List;

public class InsertDataRequest implements Serializable {
    private Record[] data;
    private List<String> tables;

    public InsertDataRequest(Record[] data, List<String> table) {
        this.data = data;
        this.tables = table;
    }

    public Record[] getData() {
        return data;
    }

    public List<String> getTable() {
        return tables;
    }
}
