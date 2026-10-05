package com.pulsepass.exception;

// Para operaciones inválidas según el negocio (ej. Usuario menor de edad)
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}