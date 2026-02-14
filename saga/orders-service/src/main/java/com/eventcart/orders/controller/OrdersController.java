package com.eventcart.orders.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.eventcart.orders.dto.CreateOrderRequest;
import com.eventcart.orders.dto.CreateOrderResponse;
import com.eventcart.orders.dto.OrderHistoryResponse;
import com.eventcart.orders.entity.OrderEntity;
import com.eventcart.orders.service.OrderHistoryService;
import com.eventcart.orders.service.OrderService;
import com.eventcart.repo.dto.Order;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrdersController {

	private final OrderService orderService;
	
	private final OrderHistoryService orderHistoryService;
	
	private Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	

	public OrdersController(OrderService orderService, OrderHistoryService orderHistoryService) {
		super();
		this.orderService = orderService;
		this.orderHistoryService = orderHistoryService;
	}



	@PostMapping
	@ResponseStatus(code = HttpStatus.ACCEPTED)
	public CreateOrderResponse placeOrder(@RequestBody @Valid CreateOrderRequest createOrderRequest) {

		LOGGER.info("In Order Controller, requestObject: {}", createOrderRequest);
		Order order = new Order();
		BeanUtils.copyProperties(createOrderRequest, order);

		Order createdOrder = orderService.placeOrder(order);

		CreateOrderResponse createOrderResponse = new CreateOrderResponse();
		BeanUtils.copyProperties(createdOrder, createOrderResponse);
		return createOrderResponse;
	}
	
	@GetMapping("/{orderId}")
	public OrderEntity getOrderById(@PathVariable String orderId) {
		return orderService.findById(orderId);
	}
	
	
	@GetMapping("/uuid")
	public String getUUID() {
		return UUID.randomUUID().toString();
	}
	
	
	@GetMapping("/history/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    public List<OrderHistoryResponse> getOrderHistory(@PathVariable String orderId) {
        return orderHistoryService.findByOrderId(orderId).stream().map(orderHistory -> {
            OrderHistoryResponse orderHistoryResponse = new OrderHistoryResponse();
            BeanUtils.copyProperties(orderHistory, orderHistoryResponse);
            return orderHistoryResponse;
        }).toList();
    }

}
