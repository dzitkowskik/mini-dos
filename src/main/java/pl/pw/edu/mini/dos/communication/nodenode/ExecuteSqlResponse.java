package pl.pw.edu.mini.dos.communication.nodenode;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.io.Serializable;

/**
 * Created by Karol Dzitkowski on 03.12.2015.
 */
public class ExecuteSqlResponse implements Serializable {
    public ErrorEnum errorType;
    public String result;


    public ExecuteSqlResponse(ErrorEnum errorType, String result) {
        this.errorType = errorType;
        this.result = result;
    }
}
