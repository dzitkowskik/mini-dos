package pl.pw.edu.mini.dos.communication.nodemaster;

public class RegisterRequest {
    /** IP address */
    String host;

    public RegisterRequest(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }
}
