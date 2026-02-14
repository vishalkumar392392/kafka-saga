package com.eventcart.products.dto;

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
public class ProductCreationResponse {
	
	private String id;
    private String name;
    private BigDecimal price;
    private Integer quantity;

}
