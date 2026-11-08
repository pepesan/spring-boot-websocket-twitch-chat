package com.cursosdedesarrollo.websockettwitchchat.controllers;

import com.cursosdedesarrollo.websockettwitchchat.auth.CreatorAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/account")
public class CreatorAccountController {
    private static final String GENERIC = "If the request can be completed, an email will be sent.";
    private final CreatorAccountService service;
    public CreatorAccountController(CreatorAccountService service) { this.service=service; }

    @PostMapping("/register")
    public ResponseEntity<Map<String,String>> register(@RequestBody Credentials credentials) {
        if (credentials.email()==null || credentials.password()==null || credentials.password().length()<12)
            return ResponseEntity.badRequest().body(Map.of("message", "Email and a password of at least 12 characters are required."));
        service.register(credentials.email(), credentials.password());
        return ResponseEntity.accepted().body(Map.of("message", GENERIC));
    }
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String,String>> verify(@RequestParam String token) {
        return service.verifyEmail(token) ? ResponseEntity.ok(Map.of("message", "Email verified."))
                : ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired token."));
    }
    @PostMapping("/password-reset")
    public ResponseEntity<Map<String,String>> requestReset(@RequestBody EmailRequest request) {
        if (request.email()!=null) service.requestPasswordReset(request.email());
        return ResponseEntity.accepted().body(Map.of("message", GENERIC));
    }
    @PostMapping("/password-reset/complete")
    public ResponseEntity<Map<String,String>> reset(@RequestBody PasswordReset request) {
        if (request.token()==null || request.password()==null || request.password().length()<12)
            return ResponseEntity.badRequest().body(Map.of("message", "A token and password of at least 12 characters are required."));
        return service.resetPassword(request.token(), request.password()) ? ResponseEntity.ok(Map.of("message", "Password updated."))
                : ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired token."));
    }
    public record Credentials(String email, String password) {}
    public record EmailRequest(String email) {}
    public record PasswordReset(String token, String password) {}
}
