package org.simple.exception;

public class EntityQueryException extends RuntimeException {
    public EntityQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
