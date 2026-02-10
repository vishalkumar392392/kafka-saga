package com.eventcart.productsmicroservice.products.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventcart.productsmicroservice.products.model.CreateProductRestModel;
import com.eventcart.productsmicroservice.products.service.ProductService;

@RestController
@RequestMapping("/products")
public class ProductController {

	public ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@PostMapping
	public ResponseEntity<String> createProduct(@RequestBody CreateProductRestModel product) {
		String productId = productService.createproduct(product);
		return ResponseEntity.status(HttpStatus.CREATED).body(productId);
	}
}
