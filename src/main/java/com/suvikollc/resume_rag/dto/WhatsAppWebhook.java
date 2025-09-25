package com.suvikollc.resume_rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record WhatsAppWebhook(String object, List<Entry> entry) {

	public record Entry(String id, List<Change> changes) {
	}

	public record Change(Value value, String field) {
	}

	public record Value(@JsonProperty("messaging_product") String messagingProduct, List<Contact> contacts,
			List<Message> messages) {
	}

	public record Contact(Profile profile, @JsonProperty("wa_id") String waId) {
	}

	public record Profile(String name) {
	}

	public record Message(String from, String id, Text text, String type) {
	}

	public record Text(String body) {
	}
}
