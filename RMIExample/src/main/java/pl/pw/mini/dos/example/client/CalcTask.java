package pl.pw.mini.dos.example.client;

import pl.pw.mini.dos.example.server.ServerData;
import pl.pw.mini.dos.example.server.Task;

import java.io.Serializable;

/**
 * Created by asd on 11/15/15.
 */
public class CalcTask implements Task<CalcTaskResult>, Serializable {
    int a, b;

    public CalcTask(int a, int b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public CalcTaskResult execute() {
        ServerData serverData = ServerData.GetInstance();

        CalcTaskResult result = new CalcTaskResult();
        result.mul = a * b * serverData.topSecretData;
        result.sum = a + b + serverData.topSecretData;
        result.sub = a - b - serverData.topSecretData;
        return result;
    }
}