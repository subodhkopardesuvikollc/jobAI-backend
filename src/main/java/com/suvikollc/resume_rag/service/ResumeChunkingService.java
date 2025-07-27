package com.suvikollc.resume_rag.service;

import java.util.List;

import org.springframework.ai.document.Document;

public interface ResumeChunkingService {
	
	List<Document> chunkResume(String resumeContent, String resumeFileName);

}
