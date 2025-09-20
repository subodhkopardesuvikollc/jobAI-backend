package com.suvikollc.resume_rag.service;

import java.util.List;

import com.suvikollc.resume_rag.entities.Communication;
import com.suvikollc.resume_rag.entities.Interview;
import com.suvikollc.resume_rag.entities.Interview.Status;

public interface InterviewService {

	List<String> getInterviewQuestions(String resumeId, String jdId);
	
	void saveInterviewQuestions(String resumeBlobName, String jdBlobName, List<String> questions);

	void saveInterviewQuestionsById(String resumeId, String jdId, List<String> questions);

	void updateInterviewStatus(Status status, String resumeId, String jdId);
	
	Interview getInterviewByIds(String resumeId, String jdId); 

	List<Communication> getInterviewCommunications(String resumeId, String jdId);

}
