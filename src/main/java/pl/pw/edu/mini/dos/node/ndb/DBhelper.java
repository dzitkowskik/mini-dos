package pl.pw.edu.mini.dos.node.ndb;

import com.sun.xml.internal.ws.api.message.HeaderList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pw.edu.mini.dos.Helper;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Contains common method used by DBmanager and ImDBmanager.
 */
public class DBhelper {
    private static final Logger logger = LoggerFactory.getLogger(DBhelper.class);

    /**
     * Given the types of each colums returns an array of functions with the function
     * to add to the PrepareStatement the data.
     * Ej: if columnType = TEXT --> st.setString(c, data)
     * Datatypes supported: SQLite3 (https://www.sqlite.org/datatype3.html)
     *
     * @param st           PrepareStatement
     * @param columnsTypes list with types of each column
     * @return array of Lambda functions
     */
    public static Consumer[] getSetDataFunctions(final PreparedStatement st, List<String> columnsTypes) {
        logger.debug("Get get functions for colums: " + Helper.collectionToString(columnsTypes));
        Consumer[] functions = new Consumer[columnsTypes.size()];
        for (int i = 0; i < columnsTypes.size(); i++) {
            String type = columnsTypes.get(i);
            final int col = i + 1;
            switch (type) {
                case "INTEGER":
                case "INT":
                case "TINYINT":
                case "SMALLINT":
                case "MEDIUMINT":
                case "BIGINT":
                case "INT2":
                case "INT8":
                    functions[i] = x -> {
                        try {
                            st.setLong(col, Long.parseLong(x.toString()));
                        } catch (SQLException e) {
                            logger.error("Col: " + col + ", Value: " + x + " E: " + e.getMessage());
                        }
                    };
                    break;
                case "TEXT":
                case "CHARACTER":
                case "VARCHAR":
                case "CLOB":
                    functions[i] = x -> {
                        try {
                            st.setString(col, x.toString());
                        } catch (SQLException e) {
                            logger.error("Col: " + col + ", Value: " + x + " E: " + e.getMessage());
                        }
                    };
                    break;
                case "BLOB":
                    functions[i] = x -> {
                        try {
                            st.setBlob(col, (Blob) x);
                        } catch (SQLException e) {
                            logger.error("Col: " + col + ", Value: " + x + " E: " + e.getMessage());
                        }
                    };
                    break;
                case "REAL":
                case "DOUBLE":
                case "FLOAT ":
                    functions[i] = x -> {
                        try {
                            st.setDouble(col, Double.parseDouble(x.toString()));
                        } catch (SQLException e) {
                            logger.error("Col: " + col + ", Value: " + x + " E: " + e.getMessage());
                        }
                    };
                    break;
                case "NUMERIC":
                case "DECIMAL":
                case "BOOLEAN":
                case "DATE":
                case "DATETIME ":
                    functions[i] = x -> {
                        try {
                            st.setBigDecimal(col, (BigDecimal) x);
                        } catch (SQLException e) {
                            logger.error("Col: " + col + ", Value: " + x + " E: " + e.getMessage());
                            logger.error(e.getMessage());
                        }
                    };
                    break;
            }
        }
        return functions;
    }
}
