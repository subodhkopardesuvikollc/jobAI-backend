package com.suvikollc.resume_rag.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EmailDTO {
	
	public enum status {
		SENT, FAILED, PENDING, INITIALIZED
	}
	public enum type {
		SENT, RECEIVED, GENERATED	}
	private type type;
	private status status;
	private String to;
	private String subject;
	private String body;
	private LocalDateTime createdAt;
	
	
}
