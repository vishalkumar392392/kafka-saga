package com.eventcart.repo.dto.commands;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProcessPaymentCommand {
	
	private String orderId;
	private String productId;
	private BigDecimal productPrice;
	private Integer productQuantity;

}
