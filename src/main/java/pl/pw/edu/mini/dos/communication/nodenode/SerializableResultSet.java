package pl.pw.edu.mini.dos.communication.nodenode;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class SerializableResultSet implements Serializable {
    List<String> columnsTypes;
    List<String> columnsNames;
    List<Object[]> data;

    public SerializableResultSet(
            List<String> columnsTypes, List<String> columnsNames, List<Object[]> data) {
        this.columnsTypes = columnsTypes;
        this.columnsNames = columnsNames;
        this.data = data;
    }

    public List<String> getColumnsTypes() {
        return columnsTypes;
    }

    public List<String> getColumnsNames() {
        return columnsNames;
    }

    public List<Object[]> getData() {
        return data;
    }

    @Override
    public String toString() {
        String str = "";
        if (data == null) {
            return str;
        }
        str += "Columns: " + columnsTypes.toString() + "\n";
        str += "Names: " + columnsNames.toString() + "\n";
        str += "Data (" + data.size() + " rows):";
        for (Object[] o : data) {
            str += "\n" + Arrays.toString(o);
        }
        return str;
    }
}
