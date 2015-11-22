package pl.pw.edu.mini.dos.communication;

import pl.pw.edu.mini.dos.Helper;

import java.io.Serializable;
import java.util.HashMap;

public class Record implements Serializable {
    public HashMap<String, Object> map;

    public Record(HashMap<String, Object> map) {
        this.map = map;
    }

    @Override
    public String toString() {
        return "Record:{" + Helper.MapToString(map) + "}";
    }
}
