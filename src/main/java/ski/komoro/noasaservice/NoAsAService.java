package ski.komoro.noasaservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NoAsAService {

    private static final int DEFAULT_PORT = 8080;
    private static final Random RANDOM = new Random();
    private static final ConcurrentHashMap<ByteBuffer, Integer> REQUEST_COUNT = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 120;

    static {
        // Start a task to clear the request count map every minute
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                REQUEST_COUNT::clear, 1, 1, TimeUnit.MINUTES);
    }

    public static void main(String[] args) throws IOException {
        // Get port from environment variable or use default
        final var envPort = System.getenv("PORT");
        InetSocketAddress socket = envPort != null && !envPort.isEmpty()
                ? new InetSocketAddress(Integer.parseInt(envPort))
                : new InetSocketAddress(DEFAULT_PORT);

        HttpServer server = HttpServer.create(socket, 0);
        server.createContext("/no", NoAsAService::handleRequest);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        System.out.println("No-as-a-Service is running on port " + socket.getPort() + "...");
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        if (calculateRequestCount(exchange) > MAX_REQUESTS_PER_MINUTE) {
            sendErrorResponse(exchange, 429, "Too many requests, please try again later.");
            return;
        }

        if (!"GET".equals(exchange.getRequestMethod())) {
            sendErrorResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        String reason = Responses.REASONS[RANDOM.nextInt(Responses.REASONS.length)];
        String response = String.format("{\"reason\":\"%s\"}", escapeJson(reason));

        // Send response
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }

    }

    private static Integer calculateRequestCount(final HttpExchange exchange) {
        byte[] rawIp = exchange.getRemoteAddress().getAddress().getAddress();
        String cfConnectingIp = exchange.getRequestHeaders().getFirst("CF-Connecting-IP");

        var clientIp = cfConnectingIp != null && !cfConnectingIp.isEmpty()
                ? ByteBuffer.wrap(cfConnectingIp.getBytes(StandardCharsets.UTF_8))
                : ByteBuffer.wrap(rawIp);

        var counter = REQUEST_COUNT.merge(clientIp, 1, Integer::sum);
        return counter;
    }

    private static void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        var response = String.format("{\"error\":\"%s\"}", message);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
