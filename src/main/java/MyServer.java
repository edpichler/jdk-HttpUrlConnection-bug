import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import lombok.Getter;

public class MyServer {
    public static final String EOF = "###EOF";
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    @Getter
    private StringBuffer stringReceived;

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        clientSocket = serverSocket.accept();

        stringReceived = new StringBuffer(10000);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        int inputChar;
        while ((inputChar = in.read()) > 0) {
            stringReceived.append(Character.valueOf((char) inputChar));
            if (stringReceived.toString().endsWith(MyServer.EOF)) {
                response200();
            }
        }

    }

    private void response200(){
        out.write("HTTP/1.1 200 OK\n" +
                          "Date: Mon, 14 Jun 2021 15:16:24 GMT\n" +
                          "Content-Type: application/vnd.schemaregistry.v1+json\n" +
                          "Content-Length: 8\n" +
                          "Server: Jetty(9.4.11.v20180605)\n" +
                          "\n" +
                          "{\n" +
                          "  \"id\": 1\n" +
                          "}\n" +
                          "\n" +
                          "Response code: 200 (OK); Time: 810ms; Content length: 8 bytes\n");
        out.flush();
    }
    public String stop() throws IOException {
        out.close();
        clientSocket.close();
        serverSocket.close();
        return stringReceived.toString();
    }

    public static void main(String[] args) throws IOException {
        MyServer server = new MyServer();
        server.start(6666);
    }
}
