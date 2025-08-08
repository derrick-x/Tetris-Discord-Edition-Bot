package com.tetrisbot;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.json.JSONObject;

public class GitHubAPI {    
    static final String GIT_TOKEN = System.getenv("GIT_TOKEN"); //use your GitHub token

    public static void write(String filename, String content) throws IOException {
        String apiURL = "https://api.github.com/repos/derrick-x/Tetris-Replays/contents/" + filename;
        StringBuilder jsonPayload = new StringBuilder();
        jsonPayload.append("{").append("\"message\": \"Save state\",").append("\"content\": \"").append(Base64.getEncoder().encodeToString(content.getBytes())).append("\"");
        String sha = null;
        try {
            HttpURLConnection getConn = (HttpURLConnection) new URL(apiURL).openConnection();
            getConn.setRequestMethod("GET");
            getConn.setRequestProperty("Authorization", "Bearer " + GIT_TOKEN);
            getConn.setRequestProperty("Accept", "application/vnd.github+json");
            if (getConn.getResponseCode() == 200) {
                InputStream in = getConn.getInputStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[4096];
                int nRead;
                while ((nRead = in.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                String json = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
                int shaIndex = json.indexOf("\"sha\":\"") + 7;
                sha = json.substring(shaIndex, json.indexOf("\"", shaIndex));
            }
        } catch (IOException e) {}
        if (sha != null) {
            jsonPayload.append(",\"sha\": \"").append(sha).append("\"");
        }
        jsonPayload.append("}");
        HttpURLConnection conn = (HttpURLConnection) new URL(apiURL).openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + GIT_TOKEN);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("Content-Type", "application/json");
        OutputStream os = conn.getOutputStream();
        os.write(jsonPayload.toString().getBytes(StandardCharsets.UTF_8));
        int code = conn.getResponseCode();
        System.out.println("GitHub upload response: " + code);
    }
    
    public static String read(String filename) throws IOException {
        String apiURL = "https://api.github.com/repos/derrick-x/Tetris-Replays/contents/" + filename;
        HttpURLConnection conn = (HttpURLConnection) new URL(apiURL).openConnection();
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        StringBuilder response;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
        }
        JSONObject json = new JSONObject(response.toString());
        String encodedContent = json.getString("content").replaceAll("\n", "");
        byte[] decodedBytes = Base64.getDecoder().decode(encodedContent);
        return new String(decodedBytes, "UTF-8");
    }
}
