package pl.pw.edu.mini.dos;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by asd on 11/21/15.
 */
public class Helper {
    public static <T> String ArrayToString(T[] array, String splitter) {
        if (array.length == 0) return "";

        StringBuilder stringBuilder = new StringBuilder(array[0].toString());
        for (int i = 1; i < array.length; i++) {
            stringBuilder.append(splitter);
            stringBuilder.append(array[i]);
        }
        return stringBuilder.toString();
    }

    public static <T> String ArrayToString(T[] array) {
        return ArrayToString(array, ", ");
    }

    public static <T> String MapToString(Map<String, T> map, String splitter) {
        if (map.size() == 0) return "";

        Iterator iterator = map.keySet().iterator();

        String key = (String) iterator.next();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(key);
        stringBuilder.append(":");
        stringBuilder.append(map.get(key));

        while (iterator.hasNext()) {
            key = (String) iterator.next();
            stringBuilder.append(splitter);
            stringBuilder.append(key);
            stringBuilder.append(":");
            stringBuilder.append(map.get(key));
        }
        return stringBuilder.toString();
    }

    public static <T> String MapToString(Map<String, T> map) {
        return MapToString(map, ", ");
    }
}
