package com.cursosdedesarrollo.websockettwitchchat.security;


import com.google.crypto.tink.Aead;
import com.google.crypto.tink.Configuration;
import com.google.crypto.tink.InsecureSecretKeyAccess;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.TinkJsonProtoKeysetFormat;
import com.google.crypto.tink.aead.AeadConfig2026;
import com.google.crypto.tink.aead.AesGcmParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;
import java.util.Base64;

@Component
public class TokenCipher {
    private static final Configuration TINK_CONFIGURATION = AeadConfig2026.get();
    private static final byte[] ASSOCIATED_DATA = "twitch-channel-authorization-v1".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    private final Aead aead;

    public TokenCipher(@Value("${twitch.credentials.encryption-keyset:}") String encodedKeyset) {
        if (encodedKeyset == null || encodedKeyset.isBlank()) {
            this.aead = null;
            return;
        }
        try {
            String keysetJson = new String(Base64.getDecoder().decode(encodedKeyset), java.nio.charset.StandardCharsets.UTF_8);
            KeysetHandle keysetHandle = TinkJsonProtoKeysetFormat.parseKeyset(
                    keysetJson, TINK_CONFIGURATION, InsecureSecretKeyAccess.get());
            this.aead = keysetHandle.getPrimitive(TINK_CONFIGURATION, Aead.class);
        } catch (GeneralSecurityException | RuntimeException exception) {
            throw new IllegalArgumentException("Credential encryption keyset is invalid", exception);
        }
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }
        try {
            return Base64.getEncoder().encodeToString(aead().encrypt(
                    plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8), ASSOCIATED_DATA));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt Twitch token", exception);
        }
    }

    public String decrypt(String encodedCiphertext) {
        if (encodedCiphertext == null) {
            throw new IllegalArgumentException("Encrypted token cannot be null");
        }
        try {
            byte[] ciphertext = Base64.getDecoder().decode(encodedCiphertext);
            return new String(aead().decrypt(ciphertext, ASSOCIATED_DATA), java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not decrypt Twitch token; check the encryption keyset and stored value", exception);
        }
    }

    public static String generateKeyset() {
        try {
            AesGcmParameters parameters = AesGcmParameters.builder()
                    .setKeySizeBytes(32)
                    .setIvSizeBytes(12)
                    .setTagSizeBytes(16)
                    .setVariant(AesGcmParameters.Variant.TINK)
                    .build();
            KeysetHandle keysetHandle = KeysetHandle.generateNew(parameters, TINK_CONFIGURATION);
            String keyset = TinkJsonProtoKeysetFormat.serializeKeyset(
                    keysetHandle, TINK_CONFIGURATION, InsecureSecretKeyAccess.get());
            return Base64.getEncoder().encodeToString(keyset.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not generate credential encryption keyset", exception);
        }
    }

    private Aead aead() {
        if (aead == null) {
            throw new IllegalStateException("Set TWITCH_TOKEN_ENCRYPTION_KEYSET before storing Twitch credentials");
        }
        return aead;
    }
}
