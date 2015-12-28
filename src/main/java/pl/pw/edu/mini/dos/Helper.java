package pl.pw.edu.mini.dos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class Helper {
    private static final Logger logger = LoggerFactory.getLogger(Helper.class);

    public static <T> String arrayToString(T[] array, String splitter) {
        if (array == null) return null;
        if (array.length == 0) return "";

        StringBuilder stringBuilder = new StringBuilder(array[0].toString());
        for (int i = 1; i < array.length; i++) {
            stringBuilder.append(splitter);
            stringBuilder.append(array[i]);
        }
        return stringBuilder.toString();
    }

    public static <T> String arrayToString(T[] array) {
        return arrayToString(array, ", ");
    }

    public static <T> String arrayToStringWithPrefix(T[] array, String splitter, String prefix) {
        return prefix + arrayToString(array, splitter + prefix);
    }

    public static <T> String collectionToString(Collection<T> list, String splitter) {
        if (list.size() == 0) return "";

        Iterator iterator = list.iterator();

        String value = iterator.next().toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(value);

        while (iterator.hasNext()) {
            value = iterator.next().toString();
            stringBuilder.append(splitter);
            stringBuilder.append(value);
        }
        return stringBuilder.toString();
    }

    public static <T> String collectionToString(Collection<T> list) {
        return collectionToString(list, ", ");
    }

    public static <T> String mapToString(Map<String, T> map, String splitter) {
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

    public static <T> String mapToString(Map<String, T> map) {
        return mapToString(map, ", ");
    }

    // fix for jar
    public static URL getResources(Class instance, String res) {
        URL configFileUrl = instance.getClassLoader().getResource(res);
        if (configFileUrl == null) {
            return null;
        }
        logger.debug(configFileUrl.toString());
        int pos = configFileUrl.toString().lastIndexOf(":");

        File f = new File(configFileUrl.toString().substring(pos + 1));
        logger.debug(f.getAbsolutePath());
        if (!f.exists()) {
            f = f.getParentFile().getParentFile().getParentFile().getAbsoluteFile();
            logger.debug(f.toString());
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
