package com.cursosdedesarrollo.websockettwitchchat.auth;

import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class CreatorAccountSecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(); }
    @Bean UserDetailsService creatorUsers(CreatorAccountRepository accounts) {
        return email -> accounts.findByEmail(email).filter(a -> a.isEmailVerified())
                .map(a -> User.withUsername(a.getEmail()).password(a.getPasswordHash()).roles("CREATOR").build())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Invalid credentials"));
    }
    @Bean DaoAuthenticationProvider creatorAuthenticationProvider(UserDetailsService creatorUsers, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(creatorUsers);
        provider.setPasswordEncoder(encoder);
        return provider;
    }
    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
    @Bean SecurityFilterChain creatorSecurity(HttpSecurity http, DaoAuthenticationProvider provider) throws Exception {
        http.authenticationProvider(provider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/callback/*/start").authenticated()
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/login.html",
                                "/login.css",
                                "/styles.css",
                                "/avatar.png",
                                "/favicon.ico",
                                "/websocket",
                                "/callback/*",
                                "/account/csrf",
                                "/account/login",
                                "/account/register",
                                "/account/verify-email",
                                "/account/password-reset",
                                "/account/password-reset/complete",
                                "/error")
                        .permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginProcessingUrl("/account/login").permitAll())
                .logout(logout -> logout.logoutUrl("/account/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                request -> "/account/session".equals(request.getServletPath()))
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                request -> request.getServletPath().matches("/callback/[^/]+/start")))
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()));
        return http.build();
    }
}
