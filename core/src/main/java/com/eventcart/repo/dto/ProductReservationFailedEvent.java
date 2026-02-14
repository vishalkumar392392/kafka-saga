package com.eventcart.repo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProductReservationFailedEvent {

	
	private String productId;
	private String orderId;
	private Integer productQuantity;
}
