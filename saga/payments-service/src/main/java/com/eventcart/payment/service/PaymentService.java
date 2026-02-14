package com.eventcart.payment.service;

import java.util.List;

import com.eventcart.repo.dto.Payment;

public interface PaymentService {

	List<Payment> findAll();

	Payment process(Payment payment);

}
