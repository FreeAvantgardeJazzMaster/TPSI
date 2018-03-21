import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.probeContentType;

public class tpsi_server {
    public static void main(String[] args) throws Exception {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler(args[0]));
        System.out.println("Starting server on port: " + port);
        server.start();
    }
    static class RootHandler implements HttpHandler {
        String path;
        public RootHandler(String path){
            this.path = path;
        }
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();

            String url = uri.toString().replace("/","\\");

            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(path);
            urlBuilder.append(url);
            String pathToShow = urlBuilder.toString();

            File file = new File(pathToShow);
            if (file.isFile()) {
                byte[] bytes  = new byte [(int)file.length()];

                exchange.getResponseHeaders().set("Content-Type", "application/txt");
                exchange.sendResponseHeaders(200, file.length());
                OutputStream os = exchange.getResponseBody();
                os.write(bytes, 0, bytes.length);
                os.close();

            }
            else {
                File[] listOfFiles = getListOfFiles(pathToShow);
                String response = getHtml(listOfFiles, uri.toString());

                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }
    private static File[] getListOfFiles(String path){
        File folder = new File(path);
        return folder.listFiles();
    }
    private static String getHtml(File[] files, String location){
        StringBuilder htmlBuilder = new StringBuilder();

        htmlBuilder.append("<html>");
        htmlBuilder.append("<head></head>");
        htmlBuilder.append("<body>");
        htmlBuilder.append("<a href=\"http://localhost:8000");
        htmlBuilder.append(location);
        htmlBuilder.append("..\">");
        htmlBuilder.append("..</a><br/>");
        for(File file : files){
            htmlBuilder.append("<a href=\"http://localhost:8000");
            //htmlBuilder.append(location.equals("/") ? "" : location);
            //htmlBuilder.append("/");
            htmlBuilder.append(location);
            htmlBuilder.append(file.getName());
            htmlBuilder.append(file.isFile() ? "\">" : "/\">");
            htmlBuilder.append(file.getName());
            htmlBuilder.append("</a>");
            htmlBuilder.append("<br/>");
        }
        htmlBuilder.append("</body>");
        htmlBuilder.append("</html>");

        return htmlBuilder.toString();
    }
}
