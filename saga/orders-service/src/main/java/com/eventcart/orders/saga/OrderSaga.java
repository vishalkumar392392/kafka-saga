package com.eventcart.orders.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.eventcart.orders.service.OrderHistoryService;
import com.eventcart.repo.dto.ProductReservedEvent;
import com.eventcart.repo.dto.commands.ApproveOrderCommand;
import com.eventcart.repo.dto.commands.CancelProductReservationCommand;
import com.eventcart.repo.dto.commands.ProcessPaymentCommand;
import com.eventcart.repo.dto.commands.ProductReservationCancelEvent;
import com.eventcart.repo.dto.commands.RejectOrderCommand;
import com.eventcart.repo.dto.commands.ReserveProductCommand;
import com.eventcart.repo.event.OrderApprovedEvent;
import com.eventcart.repo.event.OrderCreatedEvent;
import com.eventcart.repo.event.PaymentFailedEvent;
import com.eventcart.repo.event.PaymentProcessedEvent;
import com.eventcart.repo.types.OrderStatus;

@Component
@KafkaListener(topics = { "${orders.events.topic.name}", "${products.events.topic.name}",
		"${payments.events.topic.name}" })
public class OrderSaga {

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final String productsCommandTopicName;
	private final String paymentsCommandTopicName;
	private final String ordersCommandTopicName;
	private final OrderHistoryService orderHistoryService;

	private Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	public OrderSaga(KafkaTemplate<String, Object> kafkaTemplate,
			@Value("${products.commands.topic.name}") String productsCommandTopicName,
			@Value("${payments.commands.topic.name}") String paymentsCommandTopicName,
			@Value("${orders.commands.topic.name}") String ordersCommandTopicName,
			OrderHistoryService orderHistoryService) {
		this.kafkaTemplate = kafkaTemplate;
		this.productsCommandTopicName = productsCommandTopicName;
		this.orderHistoryService = orderHistoryService;
		this.paymentsCommandTopicName = paymentsCommandTopicName;
		this.ordersCommandTopicName = ordersCommandTopicName;
	}

//	@Transactional(value = "transactionManager")
	@KafkaHandler
	public void handleEvent(@Payload OrderCreatedEvent orderCreatedEvent) {

		LOGGER.info("In OrderSaga - Entered - OrderCreatedEvent: {}", orderCreatedEvent);
		ReserveProductCommand reserveProductCommand = new ReserveProductCommand();
		reserveProductCommand.setOrderId(orderCreatedEvent.getOrderId());
		reserveProductCommand.setProductId(orderCreatedEvent.getProductId());
		reserveProductCommand.setProductQuantity(orderCreatedEvent.getProductQuantity());
		LOGGER.info("In OrderSaga - Reserve Product Command sending");
		kafkaTemplate.send(productsCommandTopicName, reserveProductCommand);
		LOGGER.info("In OrderSaga - Reserve Product Command sent");
		orderHistoryService.add(orderCreatedEvent.getOrderId(), OrderStatus.CREATED);
	}

//	@Transactional(value = "transactionManager")
	@KafkaHandler
	public void handleEvent(@Payload ProductReservedEvent event) {

		LOGGER.info("In OrderSaga - Entered - ProductReservedEvent: {}", event);

		ProcessPaymentCommand command = new ProcessPaymentCommand();
		command.setOrderId(event.getOrderId());
		command.setProductId(event.getProductId());
		command.setProductPrice(event.getProductPrice());
		command.setProductQuantity(event.getProductQuantity());

		LOGGER.info("In OrderSaga - Process Payment Command sending");
		kafkaTemplate.send(paymentsCommandTopicName, command);
		LOGGER.info("In OrderSaga - Process Payment Command sent");

	}

//	@Transactional(value = "transactionManager")
	@KafkaHandler
	public void handleEvent(@Payload PaymentProcessedEvent event) {
		LOGGER.info("In OrderSaga - Entered - PaymentProcessedEvent: {}", event);

		ApproveOrderCommand command = new ApproveOrderCommand();
		command.setOrderId(event.getOrderId());

		LOGGER.info("In OrderSaga - Approve Order Command sending");
		kafkaTemplate.send(ordersCommandTopicName, command);
		LOGGER.info("In OrderSaga - Approve Order Command sent");

	}

//	@Transactional(value = "transactionManager")
	@KafkaHandler
	public void handleEvent(@Payload OrderApprovedEvent event) {

		orderHistoryService.add(event.getOrderId(), OrderStatus.APPROVED);
	}
	
//	@Transactional(value = "transactionManager")
	@KafkaHandler
	public void handleEvent(@Payload PaymentFailedEvent event) {
		
		CancelProductReservationCommand command = new CancelProductReservationCommand();
		command.setOrderId(event.getOrderId());
		command.setProductId(event.getProductId());
		command.setProductQuantity(event.getProductQuantity());
		
		LOGGER.info("In OrderSaga - CancelProductReservationCommand sending");
		kafkaTemplate.send(productsCommandTopicName, command);
		LOGGER.info("In OrderSaga - CancelProductReservationCommand sent");
	}
	
//	@Transactional(value = "transactionManager")
	@KafkaHandler
	public void handleEvent(@Payload ProductReservationCancelEvent event) {
		RejectOrderCommand command = new RejectOrderCommand();
		command.setOrderId(event.getOrderId());
		LOGGER.info("In OrderSaga - RejectOrderCommand sending");

		kafkaTemplate.send(ordersCommandTopicName, command);
		orderHistoryService.add(command.getOrderId(), OrderStatus.REJECTED);
		LOGGER.info("In OrderSaga - RejectOrderCommand Sent");

	}

}
