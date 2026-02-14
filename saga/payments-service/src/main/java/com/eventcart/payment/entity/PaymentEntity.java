package com.eventcart.payment.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table(name = "payments")
@Entity
@Data
@NoArgsConstructor
public class PaymentEntity {

	@Id
	@Column(length = 36)
	private String id;
	@Column(name = "order_id")
	private String orderId;
	@Column(name = "product_id")
	private String productId;
	@Column(name = "product_price")
	private BigDecimal productPrice;
	@Column(name = "product_quantity")
	private Integer productQuantity;

}
