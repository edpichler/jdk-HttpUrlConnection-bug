import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class HTTPTests {
    public static final int PORT = 6666;
    private MyServer myServer = new MyServer();

    @BeforeEach
    public void before() throws InterruptedException, IOException {
        new Thread(() -> {
            try {
                myServer.start(PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        Thread.sleep(200);
    }

    @AfterEach
    public void after() throws IOException {
        myServer.stop();
    }

    /**
     * Creates and send HTTP Posts using java.net.HttpURLConnection.
     */
    @ParameterizedTest
    @MethodSource({"requestBodyStringsFromFile_small", "requestBodyStringsFromFile_thousands"})
    public void postStringUsingHttpURLConnection(String requestBody) throws Exception {
        String data = requestBody + MyServer.EOF;
        postUsingHttpURLConnection(data, PORT);
        String serverReceived = myServer.getStringReceived().toString();
        assertThat(serverReceived).startsWith("POST ");
        assertThat(serverReceived).endsWith(data);
    }

    /**
     * Creates and send HTTP Posts using sockets.
     */
    @ParameterizedTest
    @MethodSource({"requestBodyStringsFromFile_small", "requestBodyStringsFromFile_thousands"})
    public void postStringUsingSocket(String requestBody) throws Exception {
        String data = requestBody + MyServer.EOF;
        postUsingSocket(data, PORT);
        String serverReceived = myServer.getStringReceived().toString();
        assertThat(serverReceived).startsWith("POST ");
        assertThat(serverReceived).endsWith(data);
    }

    /**
     * Creates and send HTTP Posts using the java.net.http.HTTPClient.
     */
    @ParameterizedTest
    @MethodSource({"requestBodyStringsFromFile_small", "requestBodyStringsFromFile_thousands"})
    public void postStringHttpClient(String requestBody) throws Exception {
        String data = requestBody + MyServer.EOF;
        postUsingHttpClient(data, PORT);
        String serverReceived = myServer.getStringReceived().toString();
        assertThat(serverReceived).startsWith("POST ");
        assertThat(serverReceived).endsWith(data);
    }

    private static Stream<Arguments> requestBodyStringsFromFile_small() throws IOException {
        return getArgumentsStream(1, 10, Optional.empty());
    }

    private static Stream<Arguments> requestBodyStringsFromFile_thousands() throws IOException {
        int start = 65_830;
        return getArgumentsStream(start, start + 10, Optional.empty());
    }

    private static Stream<Arguments> getArgumentsStream(int start, int end, final  Optional<File> fileOptional) throws IOException {
        StringBuffer bigBuffer = new StringBuffer(100_000);
        Stream<Arguments> argumentsStream = Stream.of();
        String fileToString = FileUtils.readFileToString( fileOptional.orElse(new File(MyServer.class.getResource("POST.txt").getFile())), "UTF-8");

        for (int i = start; i < end && i < (fileToString.length() -1); i++) {
            bigBuffer.append(fileToString.charAt(i));
            argumentsStream = Stream.concat(argumentsStream, Stream.of(Arguments.of(fileToString.substring(0, i))));
        }

        return argumentsStream;
    }

    //The same package and logic used in Schema Registry Client.
    private void postUsingHttpURLConnection(String requestBody, int port) throws Exception {

        java.net.HttpURLConnection connection = null;

        Map.Entry entry;
        try {
            URL url = new URL("http://localhost:" + port + "/myEndpoint");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);

            Map<String, String> stringStringMap = new HashMap<String, String>();

            Iterator i$ = stringStringMap.entrySet().iterator();

            while (i$.hasNext()) {
                entry = (Map.Entry) i$.next();
                connection.setRequestProperty((String) entry.getKey(), (String) entry.getValue());
            }
            connection.setUseCaches(false);
            if (requestBody != null) {
                connection.setDoOutput(true);
                OutputStream os = null;

                try {
                    os = connection.getOutputStream();;
                    os.write(requestBody.getBytes());
                    os.flush();
                } catch (IOException var22) {
                    throw var22;
                } finally {
                    if (os != null) {
                        os.close();
                    }
                }
            }

            int responseCode = connection.getResponseCode();
            InputStream es;
            if (responseCode != 204 && responseCode != 200) {
                es = connection.getInputStream();
                StringWriter writer = new StringWriter();
                IOUtils.copy(es, writer, "UTF-8");
                es.close();
                throw new Exception(writer.toString());
            }

            entry = null;
        } catch (Exception e) {
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // Instead of using HttpURLConnection, it uses pure socket.
    private void postUsingSocket(final String requestBody, final int port) throws Exception {

        String start = "POST /myEndpoint HTTP/1.1\n" +
                "      Cache-Control: no-cache\n" +
                "      Pragma: no-cache\n" +
                "      Host: localhost:6666\n" +
                "      Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2\n" +
                "      Connection: keep-alive\n" +
                "      Content-type: application/x-www-form-urlencoded\n" +
                "      Content-Length: ";

        Socket clientSocket = new Socket("localhost", port);
        PrintWriter out;
        BufferedReader in;
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        out.print(start);
        out.println("1000 \n\n"); //it does not matter. Mocked server.
        out.print(requestBody);
        out.flush();
        in.readLine();
        in.close(); //discard the response
        out.close();
        clientSocket.close();
    }

    // Instead of using HttpURLConnection, it uses HttpClient.
    private void postUsingHttpClient(final String requestBody, final int port) throws Exception {


        HttpClient client = HttpClient.newHttpClient();

        // create a request
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/myEndpoint"))
                                 .version(HttpClient.Version.HTTP_1_1)
                                 .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                 .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
        response.statusCode();
    }
}