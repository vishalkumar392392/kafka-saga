package com.eventcart.debit.consumer.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.eventcart.repo.event.DebitRequestedEvent;

@Component
@KafkaListener(topics = "debit-amount-topic", containerFactory = "kafkaListenerContainerFactory")
public class DebitAmountKafkahandler {

	private Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@KafkaHandler
	public void handler(@Payload DebitRequestedEvent debitRequestedEvent) {
		LOGGER.info("Received event: {} ", debitRequestedEvent.getSenderId());

	}

}
