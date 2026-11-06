package br.com.bartzeit.identity.domain;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {
    private PasswordPolicy() {
    }

    public static boolean isWithinBcryptLimit(String password) {
        return password != null && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
