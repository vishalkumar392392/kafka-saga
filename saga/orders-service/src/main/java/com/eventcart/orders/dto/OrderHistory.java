package com.eventcart.orders.dto;

import java.sql.Timestamp;

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
public class OrderHistory {

	private String id;
	private String orderId;
	private OrderStatus status;
	private Timestamp createdAt;

}
