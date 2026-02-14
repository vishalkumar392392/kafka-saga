package com.eventcart.repo.event;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderApprovedEvent {

	private String orderId;

}
