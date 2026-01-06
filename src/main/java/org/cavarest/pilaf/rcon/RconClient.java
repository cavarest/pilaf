package org.cavarest.pilaf.rcon;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * RCON client for Minecraft server management.
 * Implements the Source RCON protocol used by Minecraft servers.
 */
public class RconClient {

    private static final int SERVERDATA_AUTH = 3;
    private static final int SERVERDATA_EXECCOMMAND = 2;

    private final String host;
    private final int port;
    private final String password;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean authenticated;
    private int requestId = 1;
    private boolean verbose = false;

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public RconClient(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.authenticated = false;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(5000);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            if (authenticate()) {
                if (verbose) System.out.println("   [RCON] Connected to " + host + ":" + port);
                return true;
            } else {
                disconnect();
                return false;
            }
        } catch (IOException e) {
            System.out.println("Failed to connect to RCON server: " + e.getMessage());
            return false;
        }
    }

    private boolean authenticate() {
        try {
            int authId = requestId++;
            if (verbose) System.out.println("   [RCON] AUTH REQUEST: id=" + authId + ", type=AUTH, payload=\"" + password + "\"");
            sendPacket(authId, SERVERDATA_AUTH, password);

            RconPacket response = readPacket();
            if (response == null) {
                if (verbose) System.out.println("   [RCON] AUTH RESPONSE: FAILED - no response");
                System.out.println("Authentication failed: no response");
                return false;
            }

            if (verbose) System.out.println("   [RCON] AUTH RESPONSE: id=" + response.getRequestId() + ", type=" + response.getType() + ", body=\"" + response.getBody() + "\"");

            if (response.getRequestId() == authId || response.getRequestId() != -1) {
                authenticated = true;
                if (verbose) System.out.println("   [RCON] AUTH SUCCESS");
                return true;
            } else {
                System.out.println("Authentication failed: invalid password");
                return false;
            }
        } catch (IOException e) {
            System.out.println("Authentication failed: " + e.getMessage());
        }
        return false;
    }

    public String executeCommand(String command) {
        if (!authenticated) {
            throw new IllegalStateException("RCON client not authenticated");
        }

        try {
            int cmdId = requestId++;
            if (verbose) System.out.println("   [RCON] EXEC REQUEST: id=" + cmdId + ", type=EXEC, command=\"" + command + "\"");
            sendPacket(cmdId, SERVERDATA_EXECCOMMAND, command);

            RconPacket response = readPacket();
            if (response != null) {
                if (verbose) System.out.println("   [RCON] EXEC RESPONSE: id=" + response.getRequestId() + ", type=" + response.getType() + ", body=\"" + response.getBody() + "\"");
                // Return the body, or empty string if no response body
                return response.getBody().isEmpty() ? "(no response)" : response.getBody();
            } else {
                if (verbose) System.out.println("   [RCON] EXEC RESPONSE: FAILED - no response (command may have been sent)");
                // Return empty string instead of null for fire-and-forget commands
                return "";
            }
        } catch (IOException e) {
            if (verbose) System.out.println("   [RCON] EXEC ERROR: " + e.getMessage());
            System.out.println("Command execution failed: " + e.getMessage());
        }
        return "";
    }

    private void sendPacket(int id, int type, String body) throws IOException {
        byte[] bodyBytes = body.getBytes("UTF-8");
        int size = 4 + 4 + bodyBytes.length + 2;

        ByteBuffer buffer = ByteBuffer.allocate(4 + size);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(size);
        buffer.putInt(id);
        buffer.putInt(type);
        buffer.put(bodyBytes);
        buffer.put((byte) 0);
        buffer.put((byte) 0);

        outputStream.write(buffer.array());
        outputStream.flush();
    }

    private RconPacket readPacket() throws IOException {
        byte[] sizeBytes = new byte[4];
        if (inputStream.read(sizeBytes) != 4) {
            return null;
        }
        int size = ByteBuffer.wrap(sizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

        if (size < 10) {
            return null;
        }

        byte[] packetBytes = new byte[size];
        int totalRead = 0;
        while (totalRead < size) {
            int read = inputStream.read(packetBytes, totalRead, size - totalRead);
            if (read == -1) break;
            totalRead += read;
        }

        ByteBuffer packet = ByteBuffer.wrap(packetBytes).order(ByteOrder.LITTLE_ENDIAN);
        int requestId = packet.getInt();
        int type = packet.getInt();

        int bodyLength = size - 10;
        byte[] bodyBytes = new byte[bodyLength];
        packet.get(bodyBytes);

        String body = new String(bodyBytes, "UTF-8").trim();
        return new RconPacket(requestId, type, body);
    }

    public void disconnect() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
            authenticated = false;
            System.out.println("RCON client disconnected");
        } catch (IOException e) {
            System.out.println("Error during disconnection: " + e.getMessage());
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    private static class RconPacket {
        private final int requestId;
        private final int type;
        private final String body;

        public RconPacket(int requestId, int type, String body) {
            this.requestId = requestId;
            this.type = type;
            this.body = body;
        }

        public int getRequestId() { return requestId; }
        public int getType() { return type; }
        public String getBody() { return body; }
    }
}
