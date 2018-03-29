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
        server = HttpServer.create(new InetSocketAddress(port), 200);
        server.createContext("/", new RootHandler());
        System.out.println("Starting server on port: " + port);
        server.start();
    }
    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {

            HttpURLConnection connection = null;
            try {
                connection = makeConnection(exchange);

                serverResponse(exchange, connection);

            }catch (Exception e) {
                e.printStackTrace();
            }finally {
                if(connection != null) connection.disconnect();
            }
        }
        private HttpURLConnection makeConnection(HttpExchange exchange) throws Exception{
            HttpURLConnection connection = null;
            URL requestURL = exchange.getRequestURI().toURL();
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
            if(!connection.getRequestMethod().equals("GET")){
                connection.setDoOutput(true);
                byte[] requestBody = getBytes(exchange.getRequestBody());
                OutputStream os = connection.getOutputStream();
                os.write(requestBody);
                os.close();
            }
            System.out.println("\nSending request to URL : " + requestURL.toString());
            System.out.println("Response Code : " + connection.getResponseCode());

            return connection;
        }
        private byte[] getBytes(InputStream inputStream) throws Exception{
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            try {
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

            return buffer.toByteArray();
        }
        private void serverResponse(HttpExchange exchange, HttpURLConnection connection){
                byte[] bs = null;
            try {
                InputStream is;
                if(connection.getResponseCode() >= 400) {
                    is = connection.getErrorStream();
                }
                else {
                    is = connection.getInputStream();
                }
                if(is.available() > 0)
                    bs = getBytes(is);

                Map<String, List<String>> serverHeaders = connection.getHeaderFields();
                for (Map.Entry<String, List<String>> entry : serverHeaders.entrySet()) {
                    if (entry.getKey() != null && !entry.getKey().equalsIgnoreCase("Transfer-Encoding"))
                        exchange.getResponseHeaders().put(entry.getKey(), entry.getValue());
                }
                exchange.sendResponseHeaders(connection.getResponseCode(), ( bs != null ) ? bs.length : -1);

                if(bs!=null) {
                    OutputStream os = exchange.getResponseBody();
                    os.write(bs);
                    os.close();
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        private HttpURLConnection demoConnection() throws Exception{

            HttpURLConnection connection = null;
            URL requestURL = new URL("https://google.com");
            connection = (HttpURLConnection) requestURL.openConnection();
            connection.setRequestMethod("GET");

            System.out.println("\nSending request to URL : " + requestURL.toString());
            System.out.println("Response Code : " + connection.getResponseCode());

            return connection;
        }
    }
}