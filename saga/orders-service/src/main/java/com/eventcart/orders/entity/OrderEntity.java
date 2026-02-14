package com.eventcart.orders.entity;

import com.eventcart.repo.types.OrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "orders")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OrderEntity {

	@Id
	@Column(length = 36)
    private String id;
    @Column(name = "status")
    private OrderStatus status;
    @Column(name = "customer_id")
    private String customerId;
    @Column(name = "product_id")
    private String productId;
    @Column(name = "product_quantity")
    private Integer productQuantity;
}
