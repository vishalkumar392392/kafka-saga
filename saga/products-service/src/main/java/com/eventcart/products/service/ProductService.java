package com.eventcart.products.service;

import java.util.List;

import com.eventcart.repo.dto.Product;

public interface ProductService {

	List<Product> findAll();

	Product save(Product product);
	
	Product reserve(Product desiredProduct, String orderId);
	
	void cancelReservation(Product productTocancel, String orderId);

}
