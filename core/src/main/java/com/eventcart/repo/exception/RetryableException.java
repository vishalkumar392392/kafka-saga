package com.eventcart.repo.exception;

public class RetryableException extends RuntimeException {

	private static final long serialVersionUID = -3463082118998497308L;

	public RetryableException(String message) {
		super(message);
	}

	public RetryableException(Throwable cause) {
		super(cause);
	}

}
