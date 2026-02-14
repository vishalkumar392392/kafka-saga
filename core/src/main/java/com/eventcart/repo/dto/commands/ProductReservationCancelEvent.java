package com.eventcart.repo.dto.commands;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProductReservationCancelEvent {

	private String orderId;
	private String productId;
}
