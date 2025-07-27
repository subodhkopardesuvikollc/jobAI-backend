package com.suvikollc.resume_rag.entities;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

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

	public Resume(ObjectId id, String fileName, String blobName, LocalDateTime createdAt, LocalDateTime updatedAt) {
		super(id, fileName, blobName, createdAt, updatedAt);

	}

	public String toString() {
		return "Resume [id=" + getId() + ", fileName=" + getFileName() + ", blobName=" + getBlobName() + ", createdAt="
				+ getCreatedAt() + ", updatedAt=" + getUpdatedAt() + "]";
	}

}
