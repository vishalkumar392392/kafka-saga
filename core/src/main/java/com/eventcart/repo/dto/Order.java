package com.eventcart.repo.dto;

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
public class Order {

	private String orderId;
    private String customerId;
    private String productId;
    private Integer productQuantity;
    private OrderStatus status;
}
