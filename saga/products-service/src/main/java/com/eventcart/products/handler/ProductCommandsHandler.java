package com.eventcart.products.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.eventcart.products.service.ProductService;
import com.eventcart.repo.dto.Product;
import com.eventcart.repo.dto.ProductReservationFailedEvent;
import com.eventcart.repo.dto.ProductReservedEvent;
import com.eventcart.repo.dto.commands.CancelProductReservationCommand;
import com.eventcart.repo.dto.commands.ProductReservationCancelEvent;
import com.eventcart.repo.dto.commands.ReserveProductCommand;

@Component
@KafkaListener(topics = { "${products.commands.topic.name}" })
public class ProductCommandsHandler {

	private final ProductService productService;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final String productsEventsTopicName;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public ProductCommandsHandler(ProductService productService, KafkaTemplate<String, Object> kafkaTemplate,
			@Value("${products.events.topic.name}") String productsEventsTopicName) {
		this.productService = productService;
		this.kafkaTemplate = kafkaTemplate;
		this.productsEventsTopicName = productsEventsTopicName;
	}

//	@Transactional(value = "transactionManager")
	@KafkaHandler
	public void handler(@Payload ReserveProductCommand reserveProductCommand) {

		logger.info("In ProductCommand Handler - In handler Method");

		Product desiredProduct = new Product();
		desiredProduct.setQuantity(reserveProductCommand.getProductQuantity());
		desiredProduct.setId(reserveProductCommand.getProductId());

		try {
			Product reservedproduct = productService.reserve(desiredProduct, reserveProductCommand.getOrderId());
			ProductReservedEvent productReservedEvent = new ProductReservedEvent();

			productReservedEvent.setOrderId(reserveProductCommand.getOrderId());
			productReservedEvent.setProductId(reserveProductCommand.getProductId());
			productReservedEvent.setProductPrice(reservedproduct.getPrice());
			productReservedEvent.setProductQuantity(reserveProductCommand.getProductQuantity());
			logger.info("In ProductCommand Handler - Sending ProductEvent to Saga");
			kafkaTemplate.send(productsEventsTopicName, productReservedEvent);
			logger.info("In ProductCommand Handler - Sent ProductEvent to Saga");

		} catch (Exception e) {
			logger.error(e.getLocalizedMessage(), e);

			ProductReservationFailedEvent failedEvent = new ProductReservationFailedEvent();
			failedEvent.setOrderId(reserveProductCommand.getOrderId());
			failedEvent.setProductId(reserveProductCommand.getProductId());
			failedEvent.setProductQuantity(reserveProductCommand.getProductQuantity());
			logger.info("In ProductCommand Handler - In handler Method");

			kafkaTemplate.send(productsEventsTopicName, failedEvent);
		}

	}
	
//	@Transactional(value = "transactionManager")
	@KafkaHandler
	public void handleCommand(@Payload CancelProductReservationCommand command) {
		
		Product productToCancel = new Product();
		productToCancel.setId(command.getProductId());
		productToCancel.setQuantity(command.getProductQuantity());
		
		productService.cancelReservation(productToCancel, command.getOrderId());
		
		ProductReservationCancelEvent productReservationCancelEvent = new ProductReservationCancelEvent();
		productReservationCancelEvent.setOrderId(command.getOrderId());
		productReservationCancelEvent.setProductId(command.getProductId());
		kafkaTemplate.send(productsEventsTopicName, productReservationCancelEvent);
	}
}
