package com.suvikollc.resume_rag.entities;

import java.time.LocalDateTime;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Document(collection = "resumes")
public class Resume extends File {
	

	private List<String> jdIds;
	public Resume(ObjectId id, String fileName, String blobName,LocalDateTime createdAt, LocalDateTime updatedAt, List<String> jdIds) {
		super(id, fileName, blobName, createdAt, updatedAt);
		this.jdIds = jdIds;
	}

	public String toString() {
		return "Resume [id=" + getId() + ", fileName=" + getFileName() + ", blobName=" + getBlobName() + ", createdAt="
				+ getCreatedAt() + ", updatedAt=" + getUpdatedAt() + "]";
	}

}
