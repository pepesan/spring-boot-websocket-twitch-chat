package com.cursosdedesarrollo.websockettwitchchat.services;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenCipherTest {
    @Test
    void encryptsAndDecryptsWithGeneratedTinkKeyset() {
        TokenCipher tokenCipher = new TokenCipher(TokenCipher.generateKeyset());
        String token = "synthetic-oauth-token";

        String firstCiphertext = tokenCipher.encrypt(token);
        String secondCiphertext = tokenCipher.encrypt(token);

        assertNotEquals(token, firstCiphertext);
        assertNotEquals(firstCiphertext, secondCiphertext);
        assertEquals(token, tokenCipher.decrypt(firstCiphertext));
    }

    @Test
    void rejectsCiphertextTampering() {
        TokenCipher tokenCipher = new TokenCipher(TokenCipher.generateKeyset());
        byte[] ciphertext = Base64.getDecoder().decode(tokenCipher.encrypt("synthetic-token"));
        ciphertext[ciphertext.length - 1] ^= 1;

        assertThrows(IllegalStateException.class,
                () -> tokenCipher.decrypt(Base64.getEncoder().encodeToString(ciphertext)));
    }

    @Test
    void missingKeysetFailsWhenEncryptionIsAttempted() {
        TokenCipher tokenCipher = new TokenCipher("");

        assertThrows(IllegalStateException.class, () -> tokenCipher.encrypt("synthetic-token"));
    }

    @Test
    void wrongKeysetCannotDecryptCiphertext() {
        TokenCipher writer = new TokenCipher(TokenCipher.generateKeyset());
        TokenCipher reader = new TokenCipher(TokenCipher.generateKeyset());

        assertThrows(IllegalStateException.class, () -> reader.decrypt(writer.encrypt("synthetic-token")));
    }

    @Test
    void malformedKeysetIsRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new TokenCipher("not-a-keyset"));
    }
}
