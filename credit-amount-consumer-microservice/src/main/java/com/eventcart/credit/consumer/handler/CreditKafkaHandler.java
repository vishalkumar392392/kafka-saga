package com.eventcart.credit.consumer.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.eventcart.repo.event.CreditRequestedEvent;

@Component
@KafkaListener(topics = "credit-amount-topic",containerFactory = "kafkaListenerContainerFactory")
public class CreditKafkaHandler {
	
private Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	
	@KafkaHandler
	public void handler(@Payload CreditRequestedEvent creditRequestedEvent) { 
		
		LOGGER.info("Received event: {} ", creditRequestedEvent.getRecepientId());

	}

}
