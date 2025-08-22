package com.suvikollc.resume_rag.service;

import java.util.List;

import org.springframework.ai.document.Document;

import com.suvikollc.resume_rag.dto.ResumeAnalysisResponseDTO;

public interface ResumeService {
	
	List<Document> retrieveRelavantCandidateWork(String resumeBlobName, String jobTitle, String jdKeywords);
	
	ResumeAnalysisResponseDTO analyzeResume(String resumeBlobName, String jdBlobName);
	

}
