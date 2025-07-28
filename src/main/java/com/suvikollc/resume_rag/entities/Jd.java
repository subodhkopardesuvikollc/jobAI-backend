package com.suvikollc.resume_rag.entities;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "job_descriptions")
@NoArgsConstructor
@Getter
@Setter
public class Jd extends File {
	
	private String keywords;

	public Jd(ObjectId id, String fileName, String blobName, Map<String, Double> resumeIds, LocalDateTime createdAt,
			LocalDateTime updatedAt) {
		super(id, fileName, blobName, createdAt, updatedAt);

	}

	public String toString() {
		return "Job Description [id=" + getId() + ", fileName=" + getFileName() + ", blobName=" + getBlobName()
				+ ", createdAt=" + getCreatedAt() + ", updatedAt=" + getUpdatedAt() + "]";
	}

}
