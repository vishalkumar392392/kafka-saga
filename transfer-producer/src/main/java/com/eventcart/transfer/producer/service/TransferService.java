package com.eventcart.transfer.producer.service;

import com.eventcart.transfer.producer.model.TransferRestModel;

public interface TransferService {

	boolean transfer(TransferRestModel transferRestModel);

}
