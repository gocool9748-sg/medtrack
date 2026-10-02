package com.medtrack.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class StaticFileHandler implements HttpHandler {
    private final String rootDir;
    private final Map<String, String> mimeTypes = new HashMap<>();

    public StaticFileHandler(String rootDir) {
        this.rootDir = rootDir;
        initMimeTypes();
    }

    private void initMimeTypes() {
        mimeTypes.put(".html", "text/html; charset=UTF-8");
        mimeTypes.put(".htm", "text/html; charset=UTF-8");
        mimeTypes.put(".css", "text/css; charset=UTF-8");
        mimeTypes.put(".js", "application/javascript; charset=UTF-8");
        mimeTypes.put(".mjs", "application/javascript; charset=UTF-8");
        mimeTypes.put(".json", "application/json; charset=UTF-8");
        mimeTypes.put(".png", "image/png");
        mimeTypes.put(".jpg", "image/jpeg");
        mimeTypes.put(".jpeg", "image/jpeg");
        mimeTypes.put(".gif", "image/gif");
        mimeTypes.put(".svg", "image/svg+xml");
        mimeTypes.put(".ico", "image/x-icon");
        mimeTypes.put(".woff", "font/woff");
        mimeTypes.put(".woff2", "font/woff2");
        mimeTypes.put(".ttf", "font/ttf");
        mimeTypes.put(".wav", "audio/wav");
        mimeTypes.put(".mp3", "audio/mpeg");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        if (path.equals("/") || path.isBlank()) {
            path = "/index.html";
        }

        // Prevent directory traversal attacks
        if (path.contains("..")) {
            JsonResponse.forbidden(exchange, "Forbidden path.");
            return;
        }

        File file = new File(rootDir, path);
        if (!file.exists() || file.isDirectory()) {
            // SPA fallback: serve index.html for non-API client routes
            file = new File(rootDir, "index.html");
            if (!file.exists()) {
                JsonResponse.notFound(exchange, "File not found: " + path);
                return;
            }
        }

        String mime = getMimeType(file.getName());
        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, must-revalidate");

        long length = file.length();
        exchange.sendResponseHeaders(200, length);
        try (OutputStream os = exchange.getResponseBody();
             FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = fis.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }
        }
    }

    private String getMimeType(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) {
            String ext = filename.substring(dot).toLowerCase();
            return mimeTypes.getOrDefault(ext, "application/octet-stream");
        }
        return "text/plain";
    }
}
