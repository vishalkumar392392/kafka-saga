package com.eventcart.payment.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.eventcart.payment.entity.PaymentEntity;
import com.eventcart.payment.repository.PaymentRepository;
import com.eventcart.payment.service.CreditCardProcessorRemoteService;
import com.eventcart.payment.service.PaymentService;
import com.eventcart.repo.dto.Payment;

@Service
public class PaymentServiceImpl implements PaymentService {

	 public static final String SAMPLE_CREDIT_CARD_NUMBER = "374245455400126";
	    private final PaymentRepository paymentRepository;
	    private final CreditCardProcessorRemoteService ccpRemoteService;
	    private Logger logger = LoggerFactory.getLogger(this.getClass());

	    public PaymentServiceImpl(PaymentRepository paymentRepository,
	                              CreditCardProcessorRemoteService ccpRemoteService) {
	        this.paymentRepository = paymentRepository;
	        this.ccpRemoteService = ccpRemoteService;
	    }

	    @Override
	    public Payment process(Payment payment) {
	        BigDecimal totalPrice = payment.getProductPrice()
	                .multiply(new BigDecimal(payment.getProductQuantity()));
	        ccpRemoteService.process(new BigInteger(SAMPLE_CREDIT_CARD_NUMBER), totalPrice);
	        PaymentEntity paymentEntity = new PaymentEntity();
	        BeanUtils.copyProperties(payment, paymentEntity);
	        paymentRepository.save(paymentEntity);

	        var processedPayment = new Payment();
	        BeanUtils.copyProperties(payment, processedPayment);
	        processedPayment.setId(paymentEntity.getId());
	        return processedPayment;
	    }

	    @Override
	    public List<Payment> findAll() {
	        return paymentRepository.findAll().stream().map(entity -> new Payment(entity.getId(), entity.getOrderId(), entity.getProductId(), entity.getProductPrice(), entity.getProductQuantity())
	        ).collect(Collectors.toList());
	    }
}
