package com.cursosdedesarrollo.websockettwitchchat.security;

public final class TokenCipherKeysetGenerator {
    private TokenCipherKeysetGenerator() {
    }

    public static void main(String[] args) {
        System.out.println(TokenCipher.generateKeyset());
    }
}
