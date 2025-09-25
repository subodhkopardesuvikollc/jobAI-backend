package com.suvikollc.resume_rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record WhatsAppSendResponse(@JsonProperty("messaging_product") String messagingProduct,
		List<ResponseContact> contacts, List<ResponseMessage> messages) {
	public record ResponseContact(String input, @JsonProperty("wa_id") String waId) {
	}

	record ResponseMessage(String id) {
	}
}
