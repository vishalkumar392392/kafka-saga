package com.eventcart.repo.exception;

public class ProductInsufficientQuantityException extends RuntimeException {
    private static final long serialVersionUID = 4271625254437576275L;
	private final String productId;
    private final String orderId;

    public ProductInsufficientQuantityException(String productId, String orderId) {
        super("Product " + productId + " has insufficient quantity in the stock for order " + orderId);
        this.productId = productId;
        this.orderId = orderId;
    }

    public String getProductId() {
        return productId;
    }

    public String getOrderId() {
        return orderId;
    }
}
