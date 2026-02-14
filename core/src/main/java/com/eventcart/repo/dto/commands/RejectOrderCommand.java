package com.eventcart.repo.dto.commands;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RejectOrderCommand {

	private String orderId;

}
