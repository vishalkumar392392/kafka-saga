package com.eventcart.repo.exception;

public class NonRetryableException extends RuntimeException {

	private static final long serialVersionUID = -6740412531457357545L;

	public NonRetryableException(String message) {
		super(message);
	}

	public NonRetryableException(Throwable cause) {
		super(cause);
	}
}
