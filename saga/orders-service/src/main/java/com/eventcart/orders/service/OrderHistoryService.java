package com.eventcart.orders.service;

import java.util.List;
import java.util.UUID;

import com.eventcart.orders.dto.OrderHistory;
import com.eventcart.repo.types.OrderStatus;

public interface OrderHistoryService {
	
	 void add(String orderId, OrderStatus orderStatus);

	List<OrderHistory> findByOrderId(String orderId);

}
