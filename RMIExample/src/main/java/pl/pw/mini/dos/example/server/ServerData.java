package pl.pw.mini.dos.example.server;

/**
 * Created by asd on 11/15/15.
 */
public class ServerData {
    public int topSecretData = 0;
    private static ServerData instance = null;

    private ServerData() {
    }

    public static ServerData GetInstance() {
        if (instance == null) {
            instance = new ServerData();
        }
        return instance;
    }
}
