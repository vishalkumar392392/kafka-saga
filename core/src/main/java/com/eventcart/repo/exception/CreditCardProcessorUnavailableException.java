package com.eventcart.repo.exception;

public class CreditCardProcessorUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 6035657747082635507L;

	public CreditCardProcessorUnavailableException(Throwable cause) {
        super(cause);
    }
}
