package pl.pw.edu.mini.dos.communication;

import pl.pw.edu.mini.dos.Helper;

import java.io.Serializable;
import java.util.HashMap;

public class Record implements Serializable {
    private HashMap<String, String> map;

    public Record(HashMap<String, String> map) {
        this.map = map;
    }

    public HashMap<String, String> getMap() {
        return map;
    }

    @Override
    public String toString() {
        return "Record:{" + Helper.MapToString(map) + "}";
    }
}
