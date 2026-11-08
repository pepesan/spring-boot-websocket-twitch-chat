package com.cursosdedesarrollo.websockettwitchchat.auth;

import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountEntity;
import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

@Component
@Profile("development")
public class DevelopmentCreatorAccountSeeder implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(DevelopmentCreatorAccountSeeder.class);

    private final CreatorAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final Clock clock;

    @Autowired
    public DevelopmentCreatorAccountSeeder(
            CreatorAccountRepository accounts,
            PasswordEncoder passwordEncoder,
            @Value("${DEVELOPMENT_CREATOR_EMAIL:}") String email,
            @Value("${DEVELOPMENT_CREATOR_PASSWORD:}") String password) {
        this(accounts, passwordEncoder, email, password, Clock.systemUTC());
    }

    DevelopmentCreatorAccountSeeder(
            CreatorAccountRepository accounts,
            PasswordEncoder passwordEncoder,
            String email,
            String password,
            Clock clock) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.length() < 12) {
            logger.info("Development creator account not seeded; configure DEVELOPMENT_CREATOR_EMAIL and a password of at least 12 characters");
            return;
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (accounts.findByEmail(normalizedEmail).isPresent()) {
            logger.info("Development creator account already exists");
            return;
        }
        CreatorAccountEntity account = new CreatorAccountEntity(
                normalizedEmail, passwordEncoder.encode(password), clock.millis());
        account.setEmailVerified(true);
        accounts.saveAndFlush(account);
        logger.info("Development creator account provisioned");
    }
}
