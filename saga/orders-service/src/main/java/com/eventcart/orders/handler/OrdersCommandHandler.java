package com.eventcart.orders.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.eventcart.orders.service.OrderService;
import com.eventcart.repo.dto.commands.ApproveOrderCommand;
import com.eventcart.repo.dto.commands.RejectOrderCommand;

@Component
@KafkaListener(topics = { "${orders.commands.topic.name}" })
public class OrdersCommandHandler {

	private final OrderService orderService;
	
	private Logger LOGGER = LoggerFactory.getLogger(this.getClass());


	public OrdersCommandHandler(OrderService orderService) {
		this.orderService = orderService;
	}

//	@Transactional(value = "transactionManager")
	@KafkaHandler
	public void handleCommand(@Payload ApproveOrderCommand command) {

		LOGGER.info("In OrdersCommandHandler");
		orderService.approveOrder(command.getOrderId());
	}
	
	
//	@Transactional(value = "transactionManager")
	@KafkaHandler
	public void handleCommand(@Payload RejectOrderCommand command) {
		LOGGER.info("In OrdersCommandHandler");
		orderService.rejectOrder(command.getOrderId());
	}

}
