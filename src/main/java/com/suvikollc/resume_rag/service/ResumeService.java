package com.suvikollc.resume_rag.service;

import java.util.List;

import org.springframework.ai.document.Document;

import com.suvikollc.resume_rag.dto.ResumeAnalysisResponseDTO;
import com.suvikollc.resume_rag.entities.Resume.ResumeIndexStatus;

public interface ResumeService {

	List<Document> retrieveRelavantCandidateWork(String resumeBlobName, String jobTitle, String jdKeywords);

	public boolean isResumeIndexed(String resumeBlobName);

	void updatedResumeIndexStatus(String resumeBlobName, ResumeIndexStatus status);

	ResumeAnalysisResponseDTO analyzeResume(String resumeBlobName, String jdBlobName);
	
	void updateResumeContactInfo(String resumeBlobName, String contactInfo);
	
	String getEmailId(String resumeBlobName);
	
	String getPhoneNo(String resumeBlobName);

}
