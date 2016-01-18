package pl.pw.edu.mini.dos.Utils;

import pl.pw.edu.mini.dos.Helper;

/**
 * Created by asd on 1/18/16.
 */
public class Settings {
    public int replicationFactor = 2;
    public int nodesCount = 10;
    public Integer[] dataCounts = null;
    public int defaultParamsCount = 3;

    public Settings getSettingsFromParams(String[] myParams) {
        replicationFactor = Integer.parseInt(myParams[0]);
        nodesCount = Integer.parseInt(myParams[1]);

        if (myParams[2].equals("null")) {
            dataCounts = null;
        } else {
            String[] data = myParams[2].split(",");
            dataCounts = new Integer[data.length];
            for (int i = 0; i < data.length; i++) {
                dataCounts[i] = Integer.valueOf(data[i]);
            }
        }

        return this;
    }

    public String[] toArrayString() {
        return new String[]{
                String.valueOf(replicationFactor),
                String.valueOf(nodesCount),
                Helper.arrayToString(dataCounts, ",")
        };
    }
}
