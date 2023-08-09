package com.cursosdedesarrollo.websockettwitchchat.controllers;

import com.cursosdedesarrollo.websockettwitchchat.repositories.TwitchConfigRepository;
import com.cursosdedesarrollo.websockettwitchchat.services.TwitchAuthService;
import com.cursosdedesarrollo.websockettwitchchat.domain.TwitchConfig;
import com.cursosdedesarrollo.websockettwitchchat.twitch.TwitchChatClient;
import com.cursosdedesarrollo.websockettwitchchat.twitch.TwitchHelixClient;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/callback")
public class CallBackController {

    @Autowired
    private TwitchConfig twitchConfig;

    @Autowired
    private TwitchAuthService twitchService;

    @Autowired
    private TwitchConfigRepository twitchConfigRepository;

    @Autowired
    private TwitchChatClient twitchChatClient;

    @Autowired
    private TwitchHelixClient twitchHelixClient;

    private static final Logger logger = LoggerFactory.getLogger(CallBackController.class);

    @GetMapping
    @PostMapping
    public ResponseEntity<String> handleCallback(@PathVariable Map<String, String> pathVariables, HttpServletRequest request, @RequestHeader("User-Agent") String userAgent,
                                         @RequestHeader(value = "Referer", required = false) String referer) {
        // Loguear información sobre la petición
        String ipAddress = getClientIpAddress(request);
        String requestUrl = getRequestUrl(request);
        Map<String, String[]> queryParams = getRequestQueryParams(request);
        Map<String, String> headers = getRequestHeaders(request);

        logger.info("Petición GET recibida en {} desde la dirección IP: {}, User-Agent: {}", requestUrl, ipAddress, userAgent);
        logger.info("URL de referencia: {}", referer);
        logger.info("Parámetros de consulta (query params): {}", mapArrayToString(queryParams));
        logger.info("Cabeceras de la petición: {}", mapToString(headers));
        logger.info("PathVariables: {}", mapToString(pathVariables));

        // Tratar de leer el contenido del cuerpo de la petición
        try {
            String requestBody = getRequestBody(request);
            logger.info("Contenido del cuerpo de la petición: {}", requestBody);
        } catch (IOException e) {
            logger.error("Error al leer el cuerpo de la petición: {}", e.getMessage());
        }

        try {
            String [] codes = queryParams.get("code");
            this.twitchConfig.setAccessToken(codes[0]);
            logger.info("TwitchConfig: Access Code: " + this.twitchConfig.getAccessToken());
            this.twitchConfigRepository.save(this.twitchConfig);
            this.twitchConfig.setOauthToken(this.twitchService.getOauthToken());
            logger.info("TwitchConfig: Oauth Code: " + this.twitchConfig.getOauthToken());
            this.twitchConfigRepository.save(this.twitchConfig);
            this.twitchChatClient.connect();
            this.twitchHelixClient.connect();
            //this.twitchChatClient.getChannelMods();
            this.twitchChatClient.connectAndJoinChannel();
            this.twitchChatClient.sendMessage("Bot activado!");
            logger.info("CallbackController: mandando primer mensaje al chat");
        }catch (Exception e){
            logger.error("No se ha podido conseguir el código en el callback");
        }
        // Aquí puedes agregar la lógica para manejar la petición GET a /callback
        // Por ejemplo, puedes devolver una respuesta o realizar alguna otra acción.


        return ResponseEntity.ok(mapToString(headers));
    }

    // Método para obtener la dirección IP del cliente
    private String getClientIpAddress(HttpServletRequest request) {
        // Implementar la lógica para obtener la dirección IP del cliente (similar al ejemplo anterior)
        return "127.0.0.1"; // Por ejemplo, aquí se devuelve una dirección IP de ejemplo.
    }

    // Método para obtener la URL de la petición actual
    private String getRequestUrl(HttpServletRequest request) {
        StringBuffer requestUrl = request.getRequestURL();
        String queryString = request.getQueryString();
        if (queryString != null) {
            requestUrl.append("?").append(queryString);
        }
        return requestUrl.toString();
    }

    // Método para obtener todos los parámetros de consulta (query params) de la petición
    private Map<String, String[]> getRequestQueryParams(HttpServletRequest request) {
        return request.getParameterMap();
    }

    // Método para obtener todas las cabeceras de la petición
    private Map<String, String> getRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.put(headerName, headerValue);
        }
        return headers;
    }

    // Método para leer el contenido del cuerpo de la petición
    private String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = request.getReader();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }
    private String mapToString(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
        }
        // Eliminamos la última coma y espacio ", " para tener una representación más limpia
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
        }
        return sb.toString();
    }
    public static String mapArrayToString(Map<String, String[]> map) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            logger.info("Key: " + key);
            stringBuilder.append(key).append(": [");
            for (int i = 0; i < values.length; i++) {
                stringBuilder.append(values[i]);
                if (i < values.length - 1) {
                    stringBuilder.append(", ");
                }
            }
            stringBuilder.append("], ");
        }
        // Eliminamos la última coma y espacio ", " para tener una representación más limpia
        if (stringBuilder.length() > 0) {
            stringBuilder.setLength(stringBuilder.length() - 2);
        }
        return stringBuilder.toString();
    }
}
