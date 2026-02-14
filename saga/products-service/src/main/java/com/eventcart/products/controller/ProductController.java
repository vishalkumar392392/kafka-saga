package com.eventcart.products.controller;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.eventcart.products.dto.ProductCreationRequest;
import com.eventcart.products.dto.ProductCreationResponse;
import com.eventcart.products.service.ProductService;
import com.eventcart.repo.dto.Product;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	@ResponseStatus(code = HttpStatus.OK)
	public List<Product> findAll() {

		return productService.findAll();

	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProductCreationResponse save(@RequestBody @Valid ProductCreationRequest request) {
		Product product = new Product();
		BeanUtils.copyProperties(request, product);
		Product result = productService.save(product);

		ProductCreationResponse productCreationResponse = new ProductCreationResponse();
		BeanUtils.copyProperties(result, productCreationResponse);
		return productCreationResponse;
	}

}
