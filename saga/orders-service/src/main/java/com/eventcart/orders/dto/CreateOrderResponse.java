package com.eventcart.orders.dto;

import com.eventcart.repo.types.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CreateOrderResponse {

	private String orderId;
	private String customerId;
	private String productId;
	private Integer productQuantity;
	private OrderStatus status;

}
