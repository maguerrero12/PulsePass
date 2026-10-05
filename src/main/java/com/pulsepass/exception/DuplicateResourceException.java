package com.pulsepass.exception;

// Para conflictos de unicidad (ej. Username ya existe)
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}