import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws IOException {
        CacheStore cacheStore = new CacheStore();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/cache", new CacheHttpHandler(cacheStore));
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        System.out.println("Server started on port 8080");
    }
}
