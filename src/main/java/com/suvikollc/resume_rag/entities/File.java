package com.suvikollc.resume_rag.entities;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class File {
	
	@Id
	private ObjectId id;
	
	private String fileName;
	
	private String blobName;
		
	@CreatedDate
	private LocalDateTime createdAt;
	
	@LastModifiedDate
	private LocalDateTime updatedAt;
	
	
	public String getId() {
		return id.toHexString();
	}
	
	public String toString() {
		return "File [id=" + id + ", fileName=" + fileName + ", blobName=" + blobName + ", createdAt=" + createdAt
				+ ", updatedAt=" + updatedAt + "]";
	}

}
