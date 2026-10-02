package com.medtrack.server;

import com.google.gson.JsonObject;
import com.medtrack.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RequestContext {
    private final HttpExchange exchange;
    private final String method;
    private final URI uri;
    private final String path;
    private final Map<String, String> queryParams;
    private String rawBody;

    public RequestContext(HttpExchange exchange) {
        this.exchange = exchange;
        this.method = exchange.getRequestMethod().toUpperCase();
        this.uri = exchange.getRequestURI();
        this.path = uri.getPath();
        this.queryParams = parseQueryParams(uri.getRawQuery());
    }

    public HttpExchange getExchange() { return exchange; }
    public String getMethod() { return method; }
    public URI getUri() { return uri; }
    public String getPath() { return path; }

    public String getQueryParam(String key) {
        return queryParams.get(key);
    }

    public String getQueryParamOrDefault(String key, String defaultValue) {
        String val = queryParams.get(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }

    public Integer getQueryParamAsInt(String key) {
        String val = queryParams.get(key);
        if (val == null || val.isBlank()) return null;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getRawBody() throws IOException {
        if (rawBody == null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                rawBody = sb.toString();
            }
        }
        return rawBody;
    }

    public <T> T getBodyAs(Class<T> clazz) throws IOException {
        String body = getRawBody();
        if (body == null || body.isBlank()) return null;
        return JsonUtils.fromJson(body, clazz);
    }

    public JsonObject getBodyAsJsonObject() throws IOException {
        String body = getRawBody();
        return JsonUtils.parseJsonObject(body);
    }

    public String getBearerToken() {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    public String getIpAddress() {
        if (exchange.getRemoteAddress() != null) {
            return exchange.getRemoteAddress().getAddress().getHostAddress();
        }
        return "127.0.0.1";
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) return params;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0 && idx < pair.length() - 1) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            } else if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                params.put(key, "");
            }
        }
        return params;
    }
}
