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
import java.util.Random;

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
    private int seed = 123;
    private Random rand = new Random(seed);

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

    public String getRandomValueFromColumn(int columnIndex) {
        int rowId = rand.nextInt(insertTableCommands.size());
        return insertTableCommands.get(rowId).split("\"")[columnIndex * 2 + 1];
    }

    public String getRandomValueFromColumn(int columnIndex, int dataCount) {
        int rowId = rand.nextInt(dataCount);
        logger.trace("Random rowId= " + rowId);
        return insertTableCommands.get(rowId).split("\"")[columnIndex * 2 + 1];
    }

    public String[] getColumnsNames(String tableName) {
        String[] colNames = null;

        for (String createTableCommand : createTableCommands) {
            if (createTableCommand.indexOf(tableName) < 15) {
                String[] tmp = createTableCommand.split("`");
                colNames = new String[(tmp.length - 2) / 2];
                for (int i = 0; i < colNames.length; i++) {
                    colNames[i] = tmp[2 * i + 1];
                }
                break;
            }
        }

        return colNames;
    }


    public static TestData loadConfigTestDbFile(String filename) {
        List<String> commands = new ArrayList<>();

        URL path = Helper.getResources(TestData.class, filename);
        assertNotNull(path);
        logger.trace(path.getFile());
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
        logger.trace(Helper.collectionToString(commands));
        return new TestData(commands);
    }

}
