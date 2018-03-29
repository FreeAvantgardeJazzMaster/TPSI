import com.sun.deploy.net.HttpRequest;
import com.sun.net.httpserver.*;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
                connection = configureConnection(exchange);

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                byte[] bs = response.toString().getBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, bs.length);

                OutputStream os = exchange.getResponseBody();
                os.write(bs);
                os.close();
                //print result
                System.out.println(response.toString());

            }catch (Exception e) {
                e.printStackTrace();
            }finally {
                if(connection != null) connection.disconnect();
            }
        }
        private HttpURLConnection configureConnection(HttpExchange exchange) throws Exception{

            HttpURLConnection connection = null;

            URL requestURL = new URL(exchange.getRequestURI().toString());
            //URL requestURL = new URL("https://google.com");

            connection = (HttpURLConnection) requestURL.openConnection();
            connection.setRequestMethod(exchange.getRequestMethod());
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Via", server.getAddress().getHostString());
            connection.setRequestProperty("Transfer Encoding", "chunked");

            Headers headers = exchange.getRequestHeaders();
            for (String key : headers.keySet()){
                String s = headers.get(key).get(0);
                connection.setRequestProperty(key, headers.get(key).get(0));
            }

            System.out.println("\nSending 'GET' request to URL : " + requestURL.toString());
            System.out.println("Response Code : " + connection.getResponseCode());

            return connection;
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