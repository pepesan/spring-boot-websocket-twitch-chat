package com.cursosdedesarrollo.websockettwitchchat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class TwitchAPiHelixTests {
    @Value("${twitch.clientId}") String clientId;
    @Value("${twitch.clientSecret}") String clientSecret;
    @Value("${twitch.accessToken}") String accessToken;
    @Value("${twitch.oauthToken}") String oauthToken;
    @Value("${twitch.channel}") String channel;
    @Value("${twitch.state}") String state;
    String newAccessToken = "";
    String tokenEndpoint = "https://id.twitch.tv/oauth2/token";
    String authorizeEndpoint = "https://id.twitch.tv/oauth2/authorize";
    String userApiUrl = "https://api.twitch.tv/helix/users?login=" + channel;

    String broadcasterSubscriptionsEndpoint = "https://api.twitch.tv/helix/subscriptions";
    private String redirectURL = "http://localhost:3000/callback";
    private List<String> scopeList;


    @Test
    public void checkCredentials(){

        // String token = requestAccessToken(clientId, clientSecret, tokenEndpoint);
        String token = accessToken;
        System.out.println("Access token: " + token);
        this.accessToken = token;
        try {
            String userId = getUserId(channel, accessToken, clientId, userApiUrl);
            String broadcasterId = getBroadcasterId(userId, accessToken, clientId);
            System.out.println("Broadcaster ID: " + broadcasterId);
            scopeList = new ArrayList<>();
            scopeList.add("channel%3Amanage%3Apolls");
            scopeList.add("user%3read%3subscriptions");
            scopeList.add("channel%3read%3subscriptions");

            String oauthToken = getOathTokenWithPermissions(clientId, redirectURL, scopeList, state, token);
            System.out.println(oauthToken);
//                try {
//                    String response = getSubscriptions(broadcasterId, accessToken, clientId);
//                    System.out.println("Response: " + response);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String getOathTokenWithPermissions(String clientId, String redirectURL, List<String> scopeList, String state, String token)  throws IOException {
        StringBuffer scopes = new StringBuffer();
        for (int i =0;i<scopeList.size();i++){
            String scope = scopeList.get(i);
            scopes.append(scope);
            if (i<(scopeList.size()-1)){
                scopes.append("+");
            }
        }
        System.out.println("scopes: "+ scopes.toString());
        String urlParameters = "client_id=" + clientId + "&redirect_uri=" + redirectURL + "&scope=" + scopes.toString() + "&state=" + state + "&response_type=code";
        // byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
        URL url = new URL(authorizeEndpoint+"?"+urlParameters);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setUseCaches(false);



        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                // Parse the response to get the access token (Assuming the response is in JSON format)
                // You may use a JSON library (e.g., Gson, Jackson) to parse the response JSON.
                System.out.println(response.toString());
                return parseAccessToken(response.toString());
            }
        } else {
            throw new IOException("Failed to get access token. Response code: " + responseCode);
        }
    }

    private static String requestAccessToken(String clientId, String clientSecret, String tokenEndpoint) throws IOException {
        String urlParameters = "client_id=" + clientId + "&client_secret=" + clientSecret + "&grant_type=client_credentials";
        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

        URL url = new URL(tokenEndpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
        conn.setUseCaches(false);

        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postData);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                // Parse the response to get the access token (Assuming the response is in JSON format)
                // You may use a JSON library (e.g., Gson, Jackson) to parse the response JSON.
                return parseAccessToken(response.toString());
            }
        } else {
            throw new IOException("Failed to get access token. Response code: " + responseCode);
        }
    }

    private static String getSubscriptions(String broadcasterId, String accessToken, String clientId, String apiUrl) throws IOException {
        URL url = new URL(apiUrl + "?broadcaster_id=" + broadcasterId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Client-Id", clientId);

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } else {
            throw new IOException("Failed to get subscriptions. Response code: " + responseCode);
        }
    }

    private static String parseAccessToken(String jsonResponse) {
        // Implement the parsing logic here based on the JSON response structure.
        // For this specific example, the access_token is extracted as follows:
        // Assuming the JSON response contains an 'access_token' field like {"access_token": "your_access_token"}
        return jsonResponse.split("\"access_token\":\"")[1].split("\"")[0];
    }
    private static String getUserId(String streamerUsername, String accessToken, String clientId, String userApiUrl) throws IOException {
        URL url = new URL(userApiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Client-Id", clientId);

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                // Parse the response to get the user_id (Assuming the response is in JSON format)
                // You may use a JSON library (e.g., Gson, Jackson) to parse the response JSON.
                return parseUserId(response.toString());
            }
        } else {
            throw new IOException("Failed to get user ID. Response code: " + responseCode);
        }
    }

    private static String parseUserId(String jsonResponse) {
        // Implement the parsing logic here based on the JSON response structure.
        // Assuming the JSON response contains a 'data' array and the 'id' field in the first object represents the user_id.
        return jsonResponse.split("\"id\":\"")[1].split("\"")[0];
    }

    private static String getBroadcasterId(String userId, String accessToken, String clientId) throws IOException {
        String apiUrl = "https://api.twitch.tv/helix/users?id=" + userId;
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Client-Id", clientId);

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                // Parse the response to get the broadcaster_id (Assuming the response is in JSON format)
                // You may use a JSON library (e.g., Gson, Jackson) to parse the response JSON.
                return parseBroadcasterId(response.toString());
            }
        } else {
            throw new IOException("Failed to get broadcaster ID. Response code: " + responseCode);
        }
    }

    private static String parseBroadcasterId(String jsonResponse) {
        // Implement the parsing logic here based on the JSON response structure.
        // Assuming the JSON response contains a 'data' array and the 'id' field in the first object represents the broadcaster_id.
        return jsonResponse.split("\"id\":\"")[1].split("\"")[0];
    }
    private static String getSubscriptions(String broadcasterId, String accessToken, String clientId) throws IOException {
        String apiUrl = "https://api.twitch.tv/helix/subscriptions?broadcaster_id=" + broadcasterId;
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Client-Id", clientId);

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } else {
            throw new IOException("Failed to get subscriptions. Response code: " + responseCode);
        }
    }
}
