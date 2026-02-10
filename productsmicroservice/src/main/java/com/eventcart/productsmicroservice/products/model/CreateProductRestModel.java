package com.eventcart.productsmicroservice.products.model;

import java.math.BigDecimal;

public class CreateProductRestModel {
	
	private String title;
	
	private BigDecimal price;
	
	private Integer quantity;

	public CreateProductRestModel() {
		super();
	}

	public CreateProductRestModel(String title, BigDecimal price, Integer quantity) {
		super();
		this.title = title;
		this.price = price;
		this.quantity = quantity;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	@Override
	public String toString() {
		return "CreateProductRestModel [title=" + title + ", price=" + price + ", quantity=" + quantity + "]";
	}
	
	

}
