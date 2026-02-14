package com.eventcart.repo.dto;

import java.math.BigDecimal;

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
public class Payment {

	private String id;
    private String orderId;
    private String productId;
    private BigDecimal productPrice;
    private Integer productQuantity;
}
