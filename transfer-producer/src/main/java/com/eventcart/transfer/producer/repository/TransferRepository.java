package com.eventcart.transfer.producer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eventcart.transfer.producer.entity.TransferEntity;


@Repository
public interface TransferRepository extends JpaRepository<TransferEntity, String> {

}
