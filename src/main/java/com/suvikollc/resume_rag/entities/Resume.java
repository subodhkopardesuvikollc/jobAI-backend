package com.suvikollc.resume_rag.entities;

import java.time.LocalDateTime;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import com.suvikollc.resume_rag.dto.EmailDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
@Document(collection = "resumes")
public class Resume extends File {
	
	private String emailId;
	private List<EmailDTO> reachOutEmails;

	public Resume(ObjectId id, String fileName, String blobName, LocalDateTime createdAt, LocalDateTime updatedAt) {
		super(id, fileName, blobName, createdAt, updatedAt);

	}

	public String toString() {
		return "Resume [id=" + getId() + ", fileName=" + getFileName() + ", blobName=" + getBlobName() + ", createdAt="
				+ getCreatedAt() + ", updatedAt=" + getUpdatedAt() + "]";
	}

}
