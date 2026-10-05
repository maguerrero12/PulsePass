package com.pulsepass.exception;

// Para cuando el recurso no existe (ej. Venue no encontrado)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
