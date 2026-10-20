package com.bna.digitalbanking.credit.ai.service;

public class CreditAiProviderException extends RuntimeException {
    public CreditAiProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public CreditAiProviderException(String message) {
        super(message);
    }
}
