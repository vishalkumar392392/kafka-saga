package com.eventcart.emailnotification.email.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eventcart.emailnotification.email.entity.ProcessedEventEntity;


@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, Long> {

	
	ProcessedEventEntity findByMessageId(String messageId);
}
