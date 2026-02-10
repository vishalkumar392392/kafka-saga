package com.eventcart.transfer.producer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventcart.transfer.producer.model.TransferRestModel;
import com.eventcart.transfer.producer.service.TransferService;

@RestController
@RequestMapping("/transfer")
public class TransferController {
	
	@Autowired
	private TransferService transferService;
	
	@PostMapping
	public boolean transfer(@RequestBody TransferRestModel transferRestModel ) {
		return transferService.transfer(transferRestModel);
	}

}
