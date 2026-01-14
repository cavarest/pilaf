package org.cavarest.pilaf.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * HTTP client for the Pilaf Mineflayer Bridge.
 * Communicates with the Node.js bridge server to control Minecraft bots.
 */
public class MineflayerClient {
    private final String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private int timeout = 30000;
    private boolean verbose = false;

    public MineflayerClient(String host, int port) {
        this.baseUrl = "http://" + host + ":" + port;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void log(String message) {
        if (verbose) {
            System.out.println("   [HTTP] " + message);
        }
    }

    public boolean connect(String username) throws Exception {
        return connect(username, null, null);
    }

    public boolean connect(String username, String mcHost, Integer mcPort) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        if (mcHost != null) body.put("host", mcHost);
        if (mcPort != null) body.put("port", mcPort);
        String requestJson = toJson(body);
        log("POST /connect body=" + requestJson);
        Map<String, Object> result = post("/connect", body);
        String status = (String) result.get("status");
        log("Response: " + result);
        return "connected".equals(status) || "already_connected".equals(status);
    }

    public boolean disconnect(String username) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        log("POST /disconnect body=" + toJson(body));
        Map<String, Object> result = post("/disconnect", body);
        log("Response: " + result);
        return "disconnected".equals(result.get("status"));
    }

    public void command(String username, String command) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("command", command);
        log("POST /command body=" + toJson(body));
        Map<String, Object> result = post("/command", body);
        log("Response: " + result);
    }

    public void chat(String username, String message) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("message", message);
        log("POST /chat body=" + toJson(body));
        Map<String, Object> result = post("/chat", body);
        log("Response: " + result);
    }

    public void move(String username, double x, double y, double z) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("x", x);
        body.put("y", y);
        body.put("z", z);
        log("POST /move body=" + toJson(body));
        Map<String, Object> result = post("/move", body);
        log("Response: " + result);
    }

    public void equip(String username, String item, String slot) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("item", item);
        body.put("slot", slot);
        log("POST /equip body=" + toJson(body));
        Map<String, Object> result = post("/equip", body);
        log("Response: " + result);
    }

    public void use(String username, String target) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("target", target);
        log("POST /use body=" + toJson(body));
        Map<String, Object> result = post("/use", body);
        log("Response: " + result);
    }

    public Map<String, Object> getPosition(String username) throws Exception {
        log("GET /position/" + username);
        Map<String, Object> result = get("/position/" + username);
        log("Response: " + result);
        return result;
    }

    public Map<String, Object> getHealth(String username) throws Exception {
        log("GET /health/" + username);
        Map<String, Object> result = get("/health/" + username);
        log("Response: " + result);
        return result;
    }

    public Map<String, Object> getInventory(String username) throws Exception {
        log("GET /inventory/" + username);
        Map<String, Object> result = get("/inventory/" + username);
        log("Response: " + result);
        return result;
    }

    public Map<String, Object> getEntities(String username) throws Exception {
        log("GET /entities/" + username);
        Map<String, Object> result = get("/entities/" + username);
        log("Response: " + result);
        return result;
    }

    public Map<String, Object> getEntity(String entityName, String username) throws Exception {
        log("GET /entity/" + entityName + "/" + username);
        Map<String, Object> result = get("/entity/" + entityName + "/" + username);
        log("Response: " + result);
        return result;
    }

    public Map<String, Object> getEquipment(String username) throws Exception {
        log("GET /equipment/" + username);
        Map<String, Object> result = get("/equipment/" + username);
        log("Response: " + result);
        return result;
    }

    public boolean isHealthy() {
        try {
            Map<String, Object> result = get("/health");
            return "ok".equals(result.get("status"));
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> get(String path) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        return parseResponse(conn);
    }

    private Map<String, Object> post(String path, Map<String, Object> body) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);

        String json = toJson(body);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        return parseResponse(conn);
    }

    private Map<String, Object> parseResponse(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        is.close();

        if (code >= 400) {
            throw new Exception("HTTP " + code + ": " + response);
        }

        return parseJson(response);
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            // Fallback to simple JSON builder
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                Object v = entry.getValue();
                if (v instanceof String) sb.append("\"").append(v).append("\"");
                else if (v instanceof Number) sb.append(v);
                else if (v instanceof Boolean) sb.append(v);
                else sb.append("\"").append(v).append("\"");
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
    }

    private Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log("JSON parse error: " + e.getMessage() + ", falling back to simple parser");
            // Fallback to simple parser for simple cases
            return parseJsonSimple(json);
        }
    }

    private Map<String, Object> parseJsonSimple(String json) {
        Map<String, Object> result = new HashMap<>();
        json = json.trim();
        if (!json.startsWith("{")) return result;
        json = json.substring(1, json.length() - 1);
        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("\"", "");
                String val = kv[1].trim();
                if (val.startsWith("\"")) {
                    // String value
                    if (val.endsWith("\"") && val.length() > 1) {
                        result.put(key, val.substring(1, val.length() - 1));
                    } else {
                        result.put(key, val);
                    }
                } else if (val.equals("true")) {
                    result.put(key, true);
                } else if (val.equals("false")) {
                    result.put(key, false);
                } else if (val.equals("null")) {
                    result.put(key, null);
                } else if (val.startsWith("{")) {
                    // Nested object
                    result.put(key, parseJsonSimple(val));
                } else if (val.startsWith("[")) {
                    // Array
                    result.put(key, parseJsonArray(val));
                } else if (val.contains(".")) {
                    try {
                        result.put(key, Double.parseDouble(val));
                    } catch (Exception e) {
                        result.put(key, val);
                    }
                } else {
                    try {
                        result.put(key, Long.parseLong(val));
                    } catch (Exception e) {
                        result.put(key, val);
                    }
                }
            }
        }
        return result;
    }

    private List<Object> parseJsonArray(String json) {
        List<Object> result = new ArrayList<>();
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) return result;
        json = json.substring(1, json.length() - 1);
        if (json.isEmpty()) return result;

        List<String> items = new ArrayList<>();
        int depth = 0;
        int start = 0;
        boolean inQuotes = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
                else if (c == ',' && depth == 0) {
                    items.add(json.substring(start, i).trim());
                    start = i + 1;
                }
            }
        }
        items.add(json.substring(start).trim());

        for (String item : items) {
            if (item.startsWith("{")) {
                result.add(parseJsonSimple(item));
            } else if (item.startsWith("[")) {
                result.add(parseJsonArray(item));
            } else if (item.startsWith("\"")) {
                if (item.endsWith("\"") && item.length() > 1) {
                    result.add(item.substring(1, item.length() - 1));
                } else {
                    result.add(item);
                }
            } else if (item.equals("true")) {
                result.add(true);
            } else if (item.equals("false")) {
                result.add(false);
            } else if (item.equals("null")) {
                result.add(null);
            } else if (item.contains(".")) {
                try {
                    result.add(Double.parseDouble(item));
                } catch (Exception e) {
                    result.add(item);
                }
            } else {
                try {
                    result.add(Long.parseLong(item));
                } catch (Exception e) {
                    result.add(item);
                }
            }
        }
        return result;
    }
}
