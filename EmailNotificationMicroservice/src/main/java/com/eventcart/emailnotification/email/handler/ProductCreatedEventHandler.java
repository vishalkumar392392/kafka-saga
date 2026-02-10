package com.eventcart.emailnotification.email.handler;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.eventcart.emailnotification.email.entity.ProcessedEventEntity;
import com.eventcart.emailnotification.email.repository.ProcessedEventRepository;
import com.eventcart.repo.event.ProductCreatedEvent;
import com.eventcart.repo.exception.NonRetryableException;
import com.eventcart.repo.exception.RetryableException;

@Component
@KafkaListener(topics = "product-created-events-topic")
public class ProductCreatedEventHandler {

	private Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ProcessedEventRepository processedEventRepository;

	@Transactional
	@KafkaHandler
	public void handler(@Payload ProductCreatedEvent productCreatedEvent,
			@Header(value = "messageId", required = false) String messageId,
			@Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {

		LOGGER.info("messageId : " + messageId);
		LOGGER.info("messageKey: " + messageKey);

		ProcessedEventEntity event = processedEventRepository.findByMessageId(messageId);
		if (Objects.nonNull(event)) {
			LOGGER.info("Found a duplicate messege id : " + messageId);
			return;
		}

		String url = "https://jsonplaceholder.typicode.com/todos/1";

		try {
			ResponseEntity<String> forEntity = restTemplate.getForEntity(url, String.class);
			LOGGER.info("Called HTTP SERVER : " + forEntity.getBody());
			LOGGER.info("Received an event: " + productCreatedEvent);
		} catch (ResourceAccessException e) {
			LOGGER.error(e.getMessage());
			throw new RetryableException(e);
		} catch (HttpServerErrorException e) {
			LOGGER.error(e.getMessage());
			throw new RetryableException(e);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new NonRetryableException(e);
		}

		try {
			processedEventRepository.save(new ProcessedEventEntity(messageId, productCreatedEvent.getProductId()));
		} catch (DataIntegrityViolationException e) {
			LOGGER.error(e.getMessage());
			throw new NonRetryableException(e);
		}

	}

}
