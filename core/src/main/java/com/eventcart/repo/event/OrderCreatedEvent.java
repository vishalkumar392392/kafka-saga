package com.eventcart.repo.event;

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
public class OrderCreatedEvent {
	
	private String orderId;
    private String customerId;
    private String productId;
    private Integer productQuantity;

}
