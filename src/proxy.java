import com.sun.deploy.net.HttpRequest;
import com.sun.net.httpserver.*;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.nio.file.Files.probeContentType;

public class proxy{
    private static HttpServer server;
    public static void main(String[] args) throws Exception {
        int port = 8000;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        System.out.println("Starting server on port: " + port);
        server.start();
    }
    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {

            HttpURLConnection connection = null;
            try {
                connection = makeConnection(exchange);

                ServerResponse(exchange, connection);

            }catch (Exception e) {
                e.printStackTrace();
            }finally {
                if(connection != null) connection.disconnect();
            }
        }
        private HttpURLConnection makeConnection(HttpExchange exchange) throws Exception{
            HttpURLConnection connection = null;
            URL requestURL = new URL(exchange.getRequestURI().toString());
            //URL requestURL = new URL("https://google.com");

            connection = (HttpURLConnection) requestURL.openConnection();
            connection.setRequestMethod(exchange.getRequestMethod());
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Via", server.getAddress().getHostString());

            Headers headers = exchange.getRequestHeaders();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String headerKey = entry.getKey();
                List<String> headerValues = entry.getValue();
                for (String value : headerValues) {
                    if (headerKey != null)
                        connection.setRequestProperty(headerKey, value);
                }
            }

            System.out.println("\nSending 'GET' request to URL : " + requestURL.toString());
            System.out.println("Response Code : " + connection.getResponseCode());

            return connection;
        }
        private byte[] getBytes(InputStream inputStream) throws Exception{
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString().getBytes();
        }
        private void ServerResponse(HttpExchange exchange, HttpURLConnection connection) throws Exception{

            byte[] bs = getBytes(connection.getInputStream());

            Map<String, List<String>> serverHeaders = connection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : serverHeaders.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().equalsIgnoreCase("Transfer-Encoding"))
                    exchange.getResponseHeaders().set(entry.getKey(), entry.getValue().get(0));
            }

            exchange.sendResponseHeaders(connection.getResponseCode(), bs.length);

            OutputStream os = exchange.getResponseBody();
            os.write(bs);
            os.close();
        }
        private HttpURLConnection demoConnection() throws Exception{

            HttpURLConnection connection = null;
            URL requestURL = new URL("https://google.com");
            connection = (HttpURLConnection) requestURL.openConnection();
            connection.setRequestMethod("GET");

            System.out.println("\nSending 'GET' request to URL : " + requestURL.toString());
            System.out.println("Response Code : " + connection.getResponseCode());

            return connection;
        }
    }
}