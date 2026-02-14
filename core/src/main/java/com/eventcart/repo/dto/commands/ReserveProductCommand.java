package com.eventcart.repo.dto.commands;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReserveProductCommand {
	
	private String productId;
	private Integer productQuantity;
	private String orderId;

}
