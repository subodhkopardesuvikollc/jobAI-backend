package com.suvikollc.resume_rag.service;

import java.util.List;

import org.springframework.ai.document.Document;

public interface ResumeService {
	
	List<Document> retrieveRelavantCandidateWork(String resumeBlobName, String jobTitle, String jdKeywords);
	

}
