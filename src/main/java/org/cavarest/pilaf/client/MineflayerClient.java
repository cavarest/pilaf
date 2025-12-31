package org.cavarest.pilaf.client;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * HTTP client for the PILAF Mineflayer Bridge.
 * Communicates with the Node.js bridge server to control Minecraft bots.
 */
public class MineflayerClient {
    private final String baseUrl;
    private int timeout = 30000;

    public MineflayerClient(String host, int port) {
        this.baseUrl = "http://" + host + ":" + port;
    }

    public void setTimeout(int timeoutMs) { this.timeout = timeoutMs; }

    public boolean connect(String username) throws Exception {
        return connect(username, null, null);
    }

    public boolean connect(String username, String mcHost, Integer mcPort) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        if (mcHost != null) body.put("host", mcHost);
        if (mcPort != null) body.put("port", mcPort);
        Map<String, Object> result = post("/connect", body);
        return "connected".equals(result.get("status")) || "already_connected".equals(result.get("status"));
    }

    public boolean disconnect(String username) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        Map<String, Object> result = post("/disconnect", body);
        return "disconnected".equals(result.get("status"));
    }

    public void command(String username, String command) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("command", command);
        post("/command", body);
    }

    public void chat(String username, String message) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("message", message);
        post("/chat", body);
    }

    public void move(String username, double x, double y, double z) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("x", x);
        body.put("y", y);
        body.put("z", z);
        post("/move", body);
    }

    public void equip(String username, String item, String slot) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("item", item);
        body.put("slot", slot);
        post("/equip", body);
    }

    public void use(String username, String target) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("target", target);
        post("/use", body);
    }

    public Map<String, Object> getPosition(String username) throws Exception {
        return get("/position/" + username);
    }

    public Map<String, Object> getHealth(String username) throws Exception {
        return get("/health/" + username);
    }

    public Map<String, Object> getInventory(String username) throws Exception {
        return get("/inventory/" + username);
    }

    public Map<String, Object> getEntities(String username) throws Exception {
        return get("/entities/" + username);
    }

    public Map<String, Object> getEntity(String entityName, String username) throws Exception {
        return get("/entity/" + entityName + "/" + username);
    }

    public Map<String, Object> getEquipment(String username) throws Exception {
        return get("/equipment/" + username);
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
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v instanceof String) sb.append("\"").append(v).append("\"");
            else if (v instanceof Number) sb.append(v);
            else if (v instanceof Boolean) sb.append(v);
            else sb.append("\"").append(v).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private Map<String, Object> parseJson(String json) {
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
                if (val.startsWith("\"")) result.put(key, val.substring(1, val.length() - 1));
                else if (val.equals("true")) result.put(key, true);
                else if (val.equals("false")) result.put(key, false);
                else if (val.contains(".")) result.put(key, Double.parseDouble(val));
                else {
                    try { result.put(key, Long.parseLong(val)); }
                    catch (Exception e) { result.put(key, val); }
                }
            }
        }
        return result;
    }
}
