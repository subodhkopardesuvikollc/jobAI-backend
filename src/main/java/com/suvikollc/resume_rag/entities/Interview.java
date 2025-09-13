package com.suvikollc.resume_rag.entities;

import java.time.LocalDateTime;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Interview {

	@Id
	private ObjectId id;

	private ObjectId jdId;

	private ObjectId resumeId;

	private List<String> questions;

	private Status status = Status.PENDING;

	@CreatedDate
	private LocalDateTime createdAt;

	public enum Status {
		PENDING, IN_PROGRESS, COMPLETED, FAILED
	}

}
