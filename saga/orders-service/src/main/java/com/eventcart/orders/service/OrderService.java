package com.eventcart.orders.service;

import com.eventcart.orders.entity.OrderEntity;
import com.eventcart.repo.dto.Order;

public interface OrderService {

	Order placeOrder(Order order);

	OrderEntity findById(String orderId);

	void approveOrder(String orderId);

	void rejectOrder(String orderId);

}
