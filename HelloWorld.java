import java.io.*;
public class HelloWorld {

    public static void main(String[] args) {
        System.out.println("Method 1");

        PrintWriter writer = new PrintWriter(System.out);
        writer.println("Method 2");
        writer.flush();
        writer.close();
    }
}
