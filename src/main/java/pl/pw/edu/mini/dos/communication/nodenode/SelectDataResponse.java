package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;
import pl.pw.edu.mini.dos.communication.Record;

public class SelectDataResponse {
    public Record[] data;
    public ErrorEnum error;

    public SelectDataResponse(Record[] data, ErrorEnum error) {
        this.data = data;
        this.error = error;
    }
}
