package com.PaperPilot.exception;

public class AiServiceException extends RuntimeException {

    private final boolean timeout;

    public AiServiceException(String message, Throwable cause, boolean timeout) {
        super(message, cause);
        this.timeout = timeout;
    }

    public boolean isTimeout() {
        return timeout;
    }
}
