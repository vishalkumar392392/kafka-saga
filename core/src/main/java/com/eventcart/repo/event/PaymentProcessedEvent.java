package com.eventcart.repo.event;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentProcessedEvent {

	private String orderId;
	private String paymentId;

}
