package com.eventcart.transfer.producer.service.impl;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.eventcart.repo.event.CreditRequestedEvent;
import com.eventcart.repo.event.DebitRequestedEvent;
import com.eventcart.repo.exception.NonRetryableException;
import com.eventcart.repo.exception.RetryableException;
import com.eventcart.transfer.producer.entity.TransferEntity;
import com.eventcart.transfer.producer.model.TransferRestModel;
import com.eventcart.transfer.producer.repository.TransferRepository;
import com.eventcart.transfer.producer.service.TransferService;

@Service
public class TransferServiceImpl implements TransferService {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private TransferRepository transferRepository;

	private Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@Transactional("transactionManager")
	@Override
	public boolean transfer(TransferRestModel transferRestModel) {

		DebitRequestedEvent debitRequestedEvent = new DebitRequestedEvent();
		CreditRequestedEvent creditRequestedEvent = new CreditRequestedEvent();

		BeanUtils.copyProperties(transferRestModel, debitRequestedEvent);
		BeanUtils.copyProperties(transferRestModel, creditRequestedEvent);
		
		TransferEntity transferEntity = new TransferEntity();
		
		BeanUtils.copyProperties(transferRestModel, transferEntity);
		transferEntity.setTransferId(UUID.randomUUID().toString());

		try {
			
			transferRepository.save(transferEntity);
			
			kafkaTemplate.send("debit-amount-topic", transferRestModel.getSenderId(), debitRequestedEvent).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		callMockServer();

		kafkaTemplate.send("credit-amount-topic", transferRestModel.getRecepientId(), creditRequestedEvent);

		return true;
	}

	private void callMockServer() {
		String url = "https://jsonplaceholder.typicode.com/todos/1";
		
		try {
			ResponseEntity<String> forEntity = restTemplate.getForEntity(url, String.class);
			LOGGER.info("Called HTTP SERVER : " + forEntity.getBody().toString());
		} catch (ResourceAccessException e) {
			LOGGER.error(e.getMessage());
			throw new RetryableException(e);
		} catch (HttpServerErrorException e) {
			LOGGER.error(e.getMessage());
			throw new RetryableException(e);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new NonRetryableException(e);
		}
	}

}
