package com.suvikollc.resume_rag.entities;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.suvikollc.resume_rag.dto.CommunicationDTO.CommunicationType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Communication {

	@Id
	private ObjectId id;

	private ObjectId jdId;

	private ObjectId resumeId;

	private CommunicationType type;

	@CreatedDate
	private LocalDateTime timestamp;

	private Object content;

}
