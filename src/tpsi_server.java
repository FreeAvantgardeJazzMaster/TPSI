import com.sun.net.httpserver.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

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

            File[] listOfFiles = getListOfFiles(path);
            String response = getHtml(listOfFiles);

            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
    private static File[] getListOfFiles(String path){
        File folder = new File(path);
        return folder.listFiles();
    }
    private static String getHtml(File[] files){
        StringBuilder htmlBuilder = new StringBuilder();

        htmlBuilder.append("<html>");
        htmlBuilder.append("<head></head>");
        htmlBuilder.append("<body>");
        for(File file : files){
            htmlBuilder.append(file.getName());
            htmlBuilder.append("<br/>");
        }
        htmlBuilder.append("</body>");
        htmlBuilder.append("</html>");

        return htmlBuilder.toString();
    }
}
