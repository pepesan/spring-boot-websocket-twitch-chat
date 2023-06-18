package com.cursosdedesarrollo.websockettwitchchat.controllers;


import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;

@RestController
@RequestMapping("/callback")
public class CallBackController {
    private static final Logger logger = LoggerFactory.getLogger(CallBackController.class);

    @PostMapping
    public String callbackMethod(){
        return this.commonHandler();
    }
    @GetMapping
    public String postCallbackMethod(){
        return this.commonHandler();
    }

    public String commonHandler(){
        // Obtener todos los datos de la petición actual
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();

        // Construir una cadena con los datos de la petición
        StringBuilder sb = new StringBuilder();
        sb.append("Método: ").append(request.getMethod()).append("\n");
        sb.append("URL: ").append(request.getRequestURL()).append("\n");
        sb.append("Cabeceras:\n");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            sb.append(headerName).append(": ").append(headerValue).append("\n");
        }
        sb.append("Parámetros de consulta:\n");
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            String parameterValue = request.getParameter(parameterName);
            sb.append(parameterName).append(": ").append(parameterValue).append("\n");
        }
        sb.append("Cuerpo de la petición:\n");
        // Leer el cuerpo de la petición
        try {
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            // Manejar cualquier error de lectura del cuerpo de la petición
            e.printStackTrace();
        }

        logger.info(sb.toString());
        return sb.toString();
    }
}
