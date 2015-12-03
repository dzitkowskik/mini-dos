package pl.pw.edu.mini.dos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public final class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    private Config() {
    }

    public static Properties getConfig() {
        Properties props = null;
        URL configFileUrl = Config.class.getClassLoader().getResource("config.properties");

        try {
            if (configFileUrl == null) {
                throw new FileNotFoundException("Config file does not exist!");
            }

            File configFile = new File(configFileUrl.getPath());

            FileReader reader = new FileReader(configFile);
            props = new Properties();
            props.load(reader);
            reader.close();
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }
        return props;
    }
}
