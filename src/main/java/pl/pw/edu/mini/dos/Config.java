package pl.pw.edu.mini.dos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Created by Karol Dzitkowski on 02.12.2015.
 */
public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private Properties props;

    public Config() {
        URL configFileUrl = Helper.getResources(this.getClass(), "config.properties");

        try {
            if(configFileUrl == null) {
                throw new FileNotFoundException("Config file does not exist!");
            }

            File configFile = new File(configFileUrl.getPath());

            FileReader reader = new FileReader(configFile);
            this.props = new Properties();
            this.props.load(reader);
            reader.close();
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }
    }

    public String getProperty(String name) {
        return this.props.getProperty(name);
    }

    public static Config getConfig() {
        return new Config();
    }
}
