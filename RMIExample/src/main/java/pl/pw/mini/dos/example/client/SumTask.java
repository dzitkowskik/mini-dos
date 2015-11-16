package pl.pw.mini.dos.example.client;

import pl.pw.mini.dos.example.server.Task;

import java.io.Serializable;

/**
 * Created by asd on 11/15/15.
 */
public class SumTask implements Task<Integer>, Serializable {
    int a, b;

    public SumTask(int a, int b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public Integer execute() {
        return a + b;
    }
}
