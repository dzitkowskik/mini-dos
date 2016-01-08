package pl.pw.edu.mini.dos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: asd
 * Date: 1/6/16
 * Time: 2:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestData {
    public List<String> createTableCommands, insertTableCommands;
    private static final Logger logger = LoggerFactory.getLogger(TestData.class);

    public TestData(List<String> commands) {
        createTableCommands = new ArrayList<>();
        insertTableCommands = new ArrayList<>();

        for (String command : commands) {
            if (command.toUpperCase().contains("CREATE TABLE")) {
                createTableCommands.add(command);
            } else if (command.toUpperCase().contains("INSERT")) {
                insertTableCommands.add(command);
            }
        }
    }

    public String[] getTableNames() {
        String[] tableNames = new String[createTableCommands.size()];
        for (int i = 0; i < tableNames.length; i++) {
            tableNames[i] = createTableCommands.get(i).split(" ")[2];
        }
        return tableNames;
    }

    public List<String> getAllCommands() {
        ArrayList<String> all = new ArrayList<>();
        all.addAll(createTableCommands);
        all.addAll(insertTableCommands);

        return all;
    }

    @Override
    public String toString() {
        return Helper.collectionToString(getAllCommands(), "\n\t");
    }


    public static TestData loadConfigTestDbFile(String filename) {
        List<String> commands = new ArrayList<>();

        URL path = Helper.getResources(TestData.class, filename);
        assertNotNull(path);
        logger.info(path.getFile());
        File file = new File(path.getFile());

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

            String line = br.readLine();
            while (line != null) {
                commands.add(line);
                line = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        logger.info(Helper.collectionToString(commands));
        return new TestData(commands);
    }
}
