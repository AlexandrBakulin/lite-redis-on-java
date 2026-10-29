import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CacheHttpHandler implements HttpHandler {
    private final CacheStore store;

    public CacheHttpHandler(CacheStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String query = exchange.getRequestURI().getQuery();
        String key = extractKey(query);

        if (key == null || key.isBlank()) {
            sendResponse(exchange, 400, "Bad Request: Missing 'key' parameter");
            return;
        }

        try {
            switch (method) {
                case "GET" -> handleGet(exchange, key);
                case "POST" -> handlePost(exchange, key);
                case "DELETE" -> handleDelete(exchange, key);
                default -> sendResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Internal Server Error");
        }
    }

    private void handleGet(HttpExchange exchange, String key) throws IOException {
        String value = store.get(key);
        if (value != null) {
            sendResponse(exchange, 200, value);
        } else {
            sendResponse(exchange, 404, "Key Not Found or Expired");
        }
    }

    private void handlePost(HttpExchange exchange, String key) throws IOException {
        InputStream is = exchange.getRequestBody();
        String value = new String(is.readAllBytes(), StandardCharsets.UTF_8);

//        Если клиент не передал заголовок TTL, считаем по умолчанию 0 (вечный ключ).
        String ttlHeader = exchange.getRequestHeaders().getFirst("TTL-Millis");
        long ttl = ttlHeader != null ? Long.parseLong(ttlHeader) : 0;

        store.put(key, value, ttl);
        sendResponse(exchange, 201, "Created/Updated");
    }

    private void handleDelete(HttpExchange exchange, String key) throws IOException {
        store.delete(key);
        sendResponse(exchange, 204, "");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        // Нюанс HTTP: тело ответа пустое, а длина заголовка не -1, то сервер/клиент повиснут в ожидании данных
        exchange.sendResponseHeaders(statusCode, bytes.length == 0 ? -1 : bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            if (bytes.length > 0) os.write(bytes);
        }
    }

    private String extractKey(String query) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals("key")) {
                return pair[1];
            }
        }
        return null;
    }
}