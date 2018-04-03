import com.sun.net.httpserver.*;

import java.io.*;
import java.net.*;
import java.util.*;


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
        private int sentData;
        private int receivedData;
        public void handle(HttpExchange exchange) {

            HttpURLConnection connection = null;
            try {
                if(!isForbidden(exchange)) {
                    connection = makeConnection(exchange);
                    serverResponse(exchange, connection);
                    saveStatistics(exchange.getRequestURI());
                }
                else{
                    exchange.sendResponseHeaders(403, -1);
                    OutputStream os  = exchange.getResponseBody();
                    os.close();
                }

            }catch (Exception e) {
                e.printStackTrace();
            }finally {
                if(connection != null) connection.disconnect();
            }
        }
        private HttpURLConnection makeConnection(HttpExchange exchange) throws Exception{
            HttpURLConnection connection = null;
            URL requestURL = exchange.getRequestURI().toURL();

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
                sentData = requestBody.length;
            }
            else
                sentData = 0;
            System.out.println("\nSending request to URL : " + requestURL.toString());
            System.out.println("Response Code : " + connection.getResponseCode());

            return connection;
        }
        private byte[] getBytes(InputStream inputStream){
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
                    receivedData = bs.length;
                }
                else
                    receivedData = 0;
            }
            catch(Exception e){
                e.printStackTrace();
            }

        }
        private boolean isForbidden(HttpExchange exchange){
            String url = exchange.getRequestURI().toString();
            BufferedReader br;
            String line;
            String csvSplitBy = ",";
            String[] forbiddenDomain = null;

            try{
                br = new BufferedReader(new FileReader("blackList.csv"));
                while((line = br.readLine()) != null){
                    forbiddenDomain = line.split(csvSplitBy);
                }
                if(forbiddenDomain != null) {
                    for (int i = 0; i < forbiddenDomain.length; i++) {
                        if (url.contains(forbiddenDomain[i]))
                            return true;
                    }
                }
                return false;
            }
            catch (FileNotFoundException e){
                e.printStackTrace();
            }
            catch (IOException e){
                e.printStackTrace();
            }
            return false;
        }

        private void saveStatistics(URI uri){
            String csvFile = "stats.csv";
            boolean found = false;
            BufferedReader br;
            String line;
            String csvSplitBy = ",";
            String[] domain;
            List<String[]> domains = new ArrayList<>();
            try{
                br = new BufferedReader(new FileReader(csvFile));
                while((line = br.readLine()) != null){
                    domain = line.split(csvSplitBy);
                    domains.add(domain);
                }
                for(String[] record : domains){
                    if(uri.toString().contains(record[0])){
                        record[1] = (Integer.parseInt(record[1]) + 1) + "";
                        record[2] = (Integer.parseInt(record[2]) + sentData) + "";
                        record[3] = (Integer.parseInt(record[3]) + receivedData) + "";
                        found = true;
                        break;
                    }
                }
                if(!found){
                    String readDomain = uri.getHost();
                    readDomain = readDomain.startsWith("www.") ? readDomain.substring(4) : readDomain;
                    String[] newDomain = {readDomain, "1", sentData + "", receivedData + ""};
                    domains.add(newDomain);
                }
                writeToCSV(csvFile, domains);
            }catch (FileNotFoundException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        private void writeToCSV(String csv, List<String[]> domains){
            try {
                PrintWriter pw = new PrintWriter(new File(csv));
                StringBuilder sb = new StringBuilder();
                for(String[] domain : domains){
                    for(int i = 0; i < domain.length; i++){
                        sb.append(domain[i]);
                        if(i != domain.length - 1)
                            sb.append(",");
                    }
                    sb.append('\n');
                }
                pw.write(sb.toString());
                pw.close();
            }catch (FileNotFoundException e){
                e.printStackTrace();
            }
        }
    }
}