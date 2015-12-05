package pl.pw.edu.mini.dos.communication;

import java.util.Objects;

public enum ErrorEnum {
    NO_ERROR("No errors"),
    HOST_IS_UNAVAILABLE("The host is unavailable"),
    TIMEOUT_EXPIRED("The host didn't answer in time"),
    DENIAL_OF_PERMITS("No permissions to call remote method"),
    REMOTE_EXCEPTION("Exception while remote method was being executed"),
    INCORRECT_NAME("The name has no associated binding"),
    ANOTHER_ERROR("Error"),
    SQL_EXECUTION_ERROR("Error while executing sql query"),
    SQL_PARSING_ERROR("Sql cannot be parsed"),
    REGISTRING_TABLE_ERROR("The table cannot be registered in master");

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
