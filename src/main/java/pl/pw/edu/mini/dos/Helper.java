package pl.pw.edu.mini.dos;

import pl.pw.edu.mini.dos.communication.nodenode.ExecuteSqlResponse;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
 * Created by Karol Dzitkowski on 11/21/15.
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

    public static String executeSqlResponseListToString(
            List<ExecuteSqlResponse> list, String splitter) {
        if (list.size() == 0) return "";

        Iterator iterator = list.iterator();

        ExecuteSqlResponse value = (ExecuteSqlResponse) iterator.next();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Helper.ArrayToString(value.getData()));

        while (iterator.hasNext()) {
            value = (ExecuteSqlResponse) iterator.next();
            stringBuilder.append(splitter);
            stringBuilder.append(Helper.ArrayToString(value.getData()));
        }
        return stringBuilder.toString();
    }

    public static String executeSqlResponseListToString(
            List<ExecuteSqlResponse> list) {
        return executeSqlResponseListToString(list, "\n -- ");
    }

    public static <T> String CollectionToString(Collection<T> list, String splitter) {
        if (list.size() == 0) return "";

        Iterator iterator = list.iterator();

        String value = (String) iterator.next();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(value);

        while (iterator.hasNext()) {
            value = (String) iterator.next();
            stringBuilder.append(splitter);
            stringBuilder.append(value);
        }
        return stringBuilder.toString();
    }

    public static <T> String CollectionToString(Collection<T> list) {
        return CollectionToString(list, ", ");
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

    // fix for jar
    public static URL getResources(Class instance, String res) {
        URL configFileUrl = instance.getClassLoader().getResource(res);

        System.out.println(configFileUrl.toString());
        int pos =  configFileUrl.toString().lastIndexOf(":");

        File f = new File(configFileUrl.toString().substring(pos+1));
        System.out.println(f.getAbsolutePath());
        if (!f.exists()) {
            f = f.getParentFile().getParentFile().getParentFile().getAbsoluteFile();
            System.out.println(f);
            f = new File(f.getAbsolutePath()
                    + "/src/main/resources/" + res);
            try {
                return new URL("file", "localhost", f.getAbsolutePath());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        return configFileUrl;
    }
}
