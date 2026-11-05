package br.com.bartzeit.shared.error;

public class InvalidTenantException extends RuntimeException {
    public InvalidTenantException(String message) {
        super(message);
    }
}
