package com.cursosdedesarrollo.websockettwitchchat.services;

import com.cursosdedesarrollo.websockettwitchchat.domain.TwitchConfig;
import com.cursosdedesarrollo.websockettwitchchat.twitch.TwitchChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TwitchAuthService {
    @Autowired
    public TwitchConfig twitchConfig;

    private static final String TOKEN_ENDPOINT = "https://id.twitch.tv/oauth2/token";

    private static final Logger logger = LoggerFactory.getLogger(TwitchAuthService.class);

    public String getOauthToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/x-www-form-urlencoded");
            RestTemplate restTemplate = new RestTemplate();
            logger.info(this.twitchConfig.getRedirectUri());
            String encodedUrl = URLEncoder.encode(this.twitchConfig.getRedirectUri(), StandardCharsets.UTF_8);
            logger.info(encodedUrl);
            HttpEntity<String> request = new HttpEntity<>(
                    "client_id=" + this.twitchConfig.getClientId()
                            + "&client_secret=" + this.twitchConfig.getClientSecret()
                            + "&code=" + this.twitchConfig.getAccessToken()
                            + "&grant_type=authorization_code"
                            + "&redirect_uri=" + encodedUrl, headers);
            logger.info(request.getBody());
            ResponseEntity<String> response = restTemplate.exchange(TOKEN_ENDPOINT, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String jsonResponse = response.getBody();
                logger.info("Request Body: {}", jsonResponse);
                int startIdx = jsonResponse.indexOf("\"access_token\":") + "\"access_token\":".length() + 1;
                int endIdx = jsonResponse.indexOf(",", startIdx);
                String oauthToken = jsonResponse.substring(startIdx, endIdx - 1);
                return oauthToken;
            } else {
                // Si la respuesta no es exitosa (código 2xx), puedes obtener más detalles del error
                logger.error("Error en la petición. Código de respuesta: {}", response.getStatusCode());
                logger.error("Cuerpo de respuesta: {}", response.getBody());
                return null;
            }
        } catch (HttpClientErrorException e) {
            // Si ocurre un error de cliente (código 4xx), puedes obtener más detalles del error
            logger.error("Error de cliente en la petición. Código de respuesta: {}", e.getRawStatusCode());
            logger.error("Cuerpo de respuesta: {}", e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            // Cualquier otra excepción que pueda ocurrir durante la llamada a la API
            logger.error("Error en la petición: {}", e.getMessage());
            return null;
        }
    }

}
