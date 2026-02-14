package com.eventcart.orders.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eventcart.orders.entity.OrderHistoryEntity;


@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistoryEntity, String> {

	List<OrderHistoryEntity> findByOrderId(String orderId);

}
