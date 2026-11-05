package br.com.bartzeit.shared.error;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
