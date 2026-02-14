package com.eventcart.payment.handler;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.eventcart.payment.service.PaymentService;
import com.eventcart.repo.dto.Payment;
import com.eventcart.repo.dto.commands.ProcessPaymentCommand;
import com.eventcart.repo.event.PaymentFailedEvent;
import com.eventcart.repo.event.PaymentProcessedEvent;
import com.eventcart.repo.exception.CreditCardProcessorUnavailableException;

@Component
@KafkaListener(topics = { "${payments.commands.topic.name}" })
public class PaymentsCommandsHandler {

	private final PaymentService paymentService;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final String paymentsEventsTopicName;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public PaymentsCommandsHandler(PaymentService paymentService, KafkaTemplate<String, Object> kafkaTemplate,
			@Value("${payments.events.topic.name}") String paymentsEventsTopicName) {
		this.paymentService = paymentService;
		this.kafkaTemplate = kafkaTemplate;
		this.paymentsEventsTopicName = paymentsEventsTopicName;
	}

//	@Transactional(value = "transactionManager")
	@KafkaHandler
	public void handleCommand(@Payload ProcessPaymentCommand command) {

		Payment payment = new Payment();
		payment.setId(UUID.randomUUID().toString());
		payment.setOrderId(command.getOrderId());
		payment.setProductId(command.getProductId());
		payment.setProductPrice(command.getProductPrice());
		payment.setProductQuantity(command.getProductQuantity());

		try {
			Payment processedPayment = paymentService.process(payment);

			PaymentProcessedEvent processedEvent = new PaymentProcessedEvent();
			processedEvent.setOrderId(processedPayment.getOrderId());
			processedEvent.setPaymentId(processedPayment.getId());
			kafkaTemplate.send(paymentsEventsTopicName, processedEvent);

		} catch (Exception e) {
			logger.error(e.getLocalizedMessage(), e);
			PaymentFailedEvent paymentFailedEvent = new PaymentFailedEvent(command.getOrderId(), command.getProductId(),
					command.getProductQuantity());
			kafkaTemplate.send(paymentsEventsTopicName, paymentFailedEvent);
		}
	}

}
