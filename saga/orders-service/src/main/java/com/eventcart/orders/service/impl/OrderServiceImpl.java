package com.eventcart.orders.service.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventcart.orders.entity.OrderEntity;
import com.eventcart.orders.repository.OrderRepository;
import com.eventcart.orders.service.OrderService;
import com.eventcart.repo.dto.Order;
import com.eventcart.repo.event.OrderApprovedEvent;
import com.eventcart.repo.event.OrderCreatedEvent;
import com.eventcart.repo.types.OrderStatus;

@Service
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;

	private final KafkaTemplate<String, Object> kafkaTemplate;

	private final String ordersEventTopicName;

	private Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	public OrderServiceImpl(OrderRepository orderRepository, KafkaTemplate<String, Object> kafkaTemplate,
			@Value("${orders.events.topic.name}") String ordersEventTopicName) {
		this.orderRepository = orderRepository;
		this.kafkaTemplate = kafkaTemplate;
		this.ordersEventTopicName = ordersEventTopicName;
	}

	@Transactional(value = "transactionManager")
	@Override
	public Order placeOrder(Order order) {

		LOGGER.info("In OrderService, requestObject");

		OrderEntity entity = new OrderEntity();
		entity.setCustomerId(order.getCustomerId());
		entity.setProductId(order.getProductId());
		entity.setProductQuantity(order.getProductQuantity());
		entity.setStatus(OrderStatus.CREATED);
		String id = UUID.randomUUID().toString();
		entity.setId(id);
		OrderEntity savedOrder = orderRepository.save(entity);
		LOGGER.info("In OrderService - order saved entity: {}", savedOrder);
		BeanUtils.copyProperties(savedOrder, order);
		order.setOrderId(id);

		OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent();
		orderCreatedEvent.setOrderId(id);
		orderCreatedEvent.setCustomerId(entity.getCustomerId());
		orderCreatedEvent.setProductId(entity.getProductId());
		orderCreatedEvent.setProductQuantity(entity.getProductQuantity());

		LOGGER.info("In OrderService - sending order event");

		kafkaTemplate.send(ordersEventTopicName, orderCreatedEvent);

		LOGGER.info("In OrderService - order event sent");

		return order;
	}

	@Override
	public OrderEntity findById(String orderId) {
		return orderRepository.findById(orderId).get();
	}

	@Override
	public void approveOrder(String orderId) {
		LOGGER.info("In OrderService - approveOrder");
		OrderEntity orderEntity = orderRepository.findById(orderId).get();
		orderEntity.setStatus(OrderStatus.APPROVED);
		LOGGER.info("In OrderService - Order approving.");
		orderRepository.save(orderEntity);
		OrderApprovedEvent event = new OrderApprovedEvent();
		event.setOrderId(orderId);
		LOGGER.info("In OrderService - Order approved.");
		LOGGER.info("Order Placed Succesfully.");
		kafkaTemplate.send(ordersEventTopicName, event);

	}

	@Override
	public void rejectOrder(String orderId) {
		LOGGER.info("In OrderService - ORDER REJECTING");
		OrderEntity orderEntity = orderRepository.findById(orderId).get();
		orderEntity.setStatus(OrderStatus.REJECTED);
		orderRepository.save(orderEntity);
		LOGGER.info("In OrderService - ORDER REJECTED");

	}

}
