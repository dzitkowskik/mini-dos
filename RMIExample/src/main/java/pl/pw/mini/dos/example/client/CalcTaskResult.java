package pl.pw.mini.dos.example.client;

import java.io.Serializable;

/**
 * Created by asd on 11/15/15.
 */
public class CalcTaskResult implements Serializable {
    public int sum = 0, sub = 0, mul = 0;

    @Override
    public String toString() {
        return " sum=" + sum
                + " sub=" + sub
                + " mul=" + mul;
    }
}
