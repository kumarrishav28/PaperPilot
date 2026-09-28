package com.PaperPilot.exception;

public class DocumentProcessingException extends RuntimeException {
    public DocumentProcessingException(String message) {
        super(message);
    }

    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentProcessingException(){
        super("An error occurred while processing the document.");
    }
}
