package pl.pw.edu.mini.dos.communication;

import java.util.Objects;

public enum ErrorEnum {
    NO_ERROR("No errors"),
    ANOTHER_ERROR("Error"),
    // RMI
    HOST_IS_UNAVAILABLE("The host is unavailable"),
    TIMEOUT_EXPIRED("The host didn't answer in time"),
    REMOTE_EXCEPTION("Exception while remote method was being executed"),
    // SQL
    SQL_EXECUTION_ERROR("Error while executing sql query"),
    SQL_PARSING_ERROR("Sql cannot be parsed"),
    TABLE_NOT_EXIST("Error at inserting data becouse the table doesn't exist"),
    // SYSTEM
    REGISTRING_TABLE_ERROR("The table cannot be registered in master"),
    NOT_ENOUGH_NODES("The number of nodes available is less than replication factor");

    private final String description;

    ErrorEnum(String description) {
        this.description = description;
    }

    public boolean equals(ErrorEnum e){
        return  this.description == e.toString();
    }

    @Override
    public String toString() {
        return this.description;
    }
}
