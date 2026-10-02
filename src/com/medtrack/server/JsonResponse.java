package com.medtrack.server;

import com.google.gson.JsonObject;
import com.medtrack.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class JsonResponse {

    public static void ok(HttpExchange exchange, Object data) throws IOException {
        JsonObject res = new JsonObject();
        res.addProperty("success", true);
        res.add("data", JsonUtils.getGson().toJsonTree(data));
        send(exchange, 200, res.toString());
    }

    public static void ok(HttpExchange exchange, String message, Object data) throws IOException {
        JsonObject res = new JsonObject();
        res.addProperty("success", true);
        res.addProperty("message", message);
        if (data != null) {
            res.add("data", JsonUtils.getGson().toJsonTree(data));
        }
        send(exchange, 200, res.toString());
    }

    public static void badRequest(HttpExchange exchange, String errorMessage) throws IOException {
        JsonObject res = new JsonObject();
        res.addProperty("success", false);
        res.addProperty("error", errorMessage);
        send(exchange, 400, res.toString());
    }

    public static void unauthorized(HttpExchange exchange, String errorMessage) throws IOException {
        JsonObject res = new JsonObject();
        res.addProperty("success", false);
        res.addProperty("error", errorMessage != null ? errorMessage : "Unauthorized access. Please login.");
        send(exchange, 401, res.toString());
    }

    public static void forbidden(HttpExchange exchange, String errorMessage) throws IOException {
        JsonObject res = new JsonObject();
        res.addProperty("success", false);
        res.addProperty("error", errorMessage != null ? errorMessage : "Access denied. Insufficient permissions.");
        send(exchange, 403, res.toString());
    }

    public static void notFound(HttpExchange exchange, String errorMessage) throws IOException {
        JsonObject res = new JsonObject();
        res.addProperty("success", false);
        res.addProperty("error", errorMessage != null ? errorMessage : "Resource not found.");
        send(exchange, 404, res.toString());
    }

    public static void error(HttpExchange exchange, String errorMessage) throws IOException {
        JsonObject res = new JsonObject();
        res.addProperty("success", false);
        res.addProperty("error", errorMessage != null ? errorMessage : "Internal server error.");
        send(exchange, 500, res.toString());
    }

    private static void send(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
