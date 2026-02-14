package com.eventcart.orders.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class CreateOrderRequest {

	@NotNull
	private String customerId;
	@NotNull
	private String productId;
	@NotNull
	@Positive
	private Integer productQuantity;
}
