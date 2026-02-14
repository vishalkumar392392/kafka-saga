package com.eventcart.products.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "products")
@Entity
public class ProductEntity {

	@Id
	@Column(length = 36)
	private String id;
	@Column(name = "quantity")
	private Integer quantity;
	@Column(name = "name")
	private String name;
	@Column(name = "price")
	private BigDecimal price;

}
