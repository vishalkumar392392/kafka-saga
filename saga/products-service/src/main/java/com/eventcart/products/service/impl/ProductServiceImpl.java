package com.eventcart.products.service.impl;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.eventcart.products.entity.ProductEntity;
import com.eventcart.products.repository.ProductRepository;
import com.eventcart.products.service.ProductService;
import com.eventcart.repo.dto.Product;
import com.eventcart.repo.exception.ProductInsufficientQuantityException;

@Service
public class ProductServiceImpl implements ProductService {

	private final ProductRepository productRepository;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());


	public ProductServiceImpl(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Override
	public List<Product> findAll() {

		return productRepository.findAll().stream().map(ProductServiceImpl::getproduct).toList();
	}

	private static Product getproduct(ProductEntity productEntity) {
		Product product = new Product();
		BeanUtils.copyProperties(productEntity, product);
		return product;
	}

	@Override
	public Product save(Product product) {
		ProductEntity productEntity = new ProductEntity();
		BeanUtils.copyProperties(product, productEntity);
		productEntity.setId(UUID.randomUUID().toString());
		return getproduct(productRepository.save(productEntity));
	}

	@Override
	public Product reserve(Product desiredProduct, String orderId) {

		logger.info("In ProductServiceImpl - Inside reserve method.");
		ProductEntity productEntity = productRepository.findById(desiredProduct.getId()).orElseThrow();
		if (desiredProduct.getQuantity() > productEntity.getQuantity()) {
			throw new ProductInsufficientQuantityException(productEntity.getId(), orderId);
		}

		productEntity.setQuantity(productEntity.getQuantity() - desiredProduct.getQuantity());
		logger.info("In ProductServiceImpl - Reserving in database.");
		productRepository.save(productEntity);
		logger.info("In ProductServiceImpl - Reserved.");
		var reservedProduct = new Product();
		BeanUtils.copyProperties(productEntity, reservedProduct);
		reservedProduct.setQuantity(desiredProduct.getQuantity());
		return reservedProduct;
	}

	@Override
	public void cancelReservation(Product productTocancel, String orderId) {

		ProductEntity productEntity = productRepository.findById(productTocancel.getId()).orElseThrow();
		productEntity.setQuantity(productEntity.getQuantity() + productTocancel.getQuantity());
		productRepository.save(productEntity);

	}

}
