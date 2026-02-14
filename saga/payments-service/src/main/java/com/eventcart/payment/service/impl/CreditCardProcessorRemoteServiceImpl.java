package com.eventcart.payment.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.eventcart.payment.service.CreditCardProcessorRemoteService;
import com.eventcart.repo.dto.CreditCardProcessRequest;
import com.eventcart.repo.exception.CreditCardProcessorUnavailableException;


@Service
public class CreditCardProcessorRemoteServiceImpl implements CreditCardProcessorRemoteService {

	private final RestTemplate restTemplate;
    private final String ccpRemoteServiceUrl;
    
    private Logger logger = LoggerFactory.getLogger(this.getClass());


    public CreditCardProcessorRemoteServiceImpl(
            RestTemplate restTemplate,
            @Value("${remote.ccp.url}") String ccpRemoteServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.ccpRemoteServiceUrl = ccpRemoteServiceUrl;
    }


    
    @Override
    public void process(BigInteger cardNumber, BigDecimal paymentAmount) {
    	CreditCardProcessRequest request = new CreditCardProcessRequest(cardNumber, paymentAmount);
        restTemplate.getForEntity(ccpRemoteServiceUrl, String.class);
        
        /** Un comment the below to test the compensating transactions.. */
//        restTemplate.getForEntity(ccpRemoteServiceUrl+"/1234", String.class);

//        try {
//        	CreditCardProcessRequest request = new CreditCardProcessRequest(cardNumber, paymentAmount);
//            restTemplate.getForEntity(ccpRemoteServiceUrl+"/345654", String.class);
//        } catch (CreditCardProcessorUnavailableException e) {
//        	logger.error(e.getLocalizedMessage(),e);
//        	throw new CreditCardProcessorUnavailableException(e);
//        }
    }
}
