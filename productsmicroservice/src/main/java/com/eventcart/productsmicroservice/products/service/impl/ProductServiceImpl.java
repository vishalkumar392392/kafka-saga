package com.eventcart.productsmicroservice.products.service.impl;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.eventcart.productsmicroservice.products.model.CreateProductRestModel;
import com.eventcart.productsmicroservice.products.service.ProductService;
import com.eventcart.repo.event.ProductCreatedEvent;

@Service
public class ProductServiceImpl implements ProductService {

	private KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

	private Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	public ProductServiceImpl(KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	/**
	 * This is asynchronous way of adding an event to kafka topic
	 * 
	 * @Override public String createproduct(CreateProductRestModel
	 *           createProductRestModel) {
	 * 
	 *           String productId = UUID.randomUUID().toString();
	 *           ProductCreatedEvent productCreatedEvent = new
	 *           ProductCreatedEvent(productId, createProductRestModel.getTitle(),
	 *           createProductRestModel.getPrice(),
	 *           createProductRestModel.getQuantity());
	 *           CompletableFuture<SendResult<String, ProductCreatedEvent>> future =
	 *           kafkaTemplate .send("product-created-events-topic", productId,
	 *           productCreatedEvent); future.whenComplete((result, exception) -> {
	 *           if (exception != null) { LOGGER.error("****************Failed to
	 *           send message: " + exception.getMessage()); } else {
	 *           LOGGER.info("*****************Message sent successfully: " +
	 *           result.getRecordMetadata()); } });
	 * 
	 *           LOGGER.info("*************Returning product id");
	 * 
	 *           return productId; }
	 * 
	 */

	@Override
	public String createproduct(CreateProductRestModel createProductRestModel) {

		String productId = UUID.randomUUID().toString();
		ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(productId, createProductRestModel.getTitle(),
				createProductRestModel.getPrice(), createProductRestModel.getQuantity());
		LOGGER.info("*************Before sending message***********");
		try {
			ProducerRecord<String, ProductCreatedEvent> record = new ProducerRecord<>("product-created-events-topic",
					productId, productCreatedEvent);
			record.headers().add("messageId", UUID.randomUUID().toString().getBytes());
			SendResult<String, ProductCreatedEvent> result = kafkaTemplate
					.send(record).get();

			LOGGER.info("Partition: " + result.getRecordMetadata().partition());
			LOGGER.info("Topic: " + result.getRecordMetadata().topic());
			LOGGER.info("Offset: " + result.getRecordMetadata().offset());

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		LOGGER.info("*************Returning product id");

		return productId;
	}

}
