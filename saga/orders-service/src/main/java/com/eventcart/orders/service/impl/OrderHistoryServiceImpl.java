package com.eventcart.orders.service.impl;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.eventcart.orders.dto.OrderHistory;
import com.eventcart.orders.entity.OrderHistoryEntity;
import com.eventcart.orders.repository.OrderHistoryRepository;
import com.eventcart.orders.service.OrderHistoryService;
import com.eventcart.repo.types.OrderStatus;

@Service
public class OrderHistoryServiceImpl implements OrderHistoryService {


	private final OrderHistoryRepository orderHistoryRepository;
	
	private Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	public OrderHistoryServiceImpl(OrderHistoryRepository orderHistoryRepository) {
		this.orderHistoryRepository = orderHistoryRepository;
	}

	@Override
	public List<OrderHistory> findByOrderId(String orderId) {
		List<OrderHistoryEntity> entites = orderHistoryRepository.findByOrderId(orderId);
		return entites.stream().map(entity -> {
			OrderHistory orderHistory = new OrderHistory();
			BeanUtils.copyProperties(entity, orderHistory);
			return orderHistory;
		}).toList();
	}

	@Override
	public void add(String orderId, OrderStatus orderStatus) {

		LOGGER.info("In OrderHistoryService - Adding/Updating order");
		OrderHistoryEntity entity = new OrderHistoryEntity();
		entity.setId(UUID.randomUUID().toString());
		entity.setOrderId(orderId);
		entity.setStatus(orderStatus);
		entity.setCreatedAt(new Timestamp(new Date().getTime()));
		orderHistoryRepository.save(entity);
		LOGGER.info("In OrderHistoryService - Added/Updated order");
	}

}
