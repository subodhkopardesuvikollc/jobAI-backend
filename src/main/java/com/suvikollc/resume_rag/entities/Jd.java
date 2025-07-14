package com.suvikollc.resume_rag.entities;

import java.time.LocalDateTime;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "job_descriptions")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Jd extends File{

	
	
	private List<String> resumeIds;
	
	public Jd(ObjectId id, String fileName, String blobName, List<String> resumeIds, LocalDateTime createdAt, LocalDateTime updatedAt) {
		super(id, fileName, blobName, createdAt, updatedAt);
		this.resumeIds = resumeIds;
		
	}
	
	public String toString() {
		return "Job Description [id=" + getId() + ", fileName=" + getFileName() + ", blobName=" + getBlobName() + ", createdAt=" + getCreatedAt()
				+ ", updatedAt=" + getUpdatedAt() + "]";
	}
	
}
