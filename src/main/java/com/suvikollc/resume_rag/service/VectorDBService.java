package com.suvikollc.resume_rag.service;

import com.suvikollc.resume_rag.dto.SearchResultsDto;

public interface VectorDBService {
	
	public SearchResultsDto getResults(String jdId);
	
	public void uploadToVectorDB(String fileName);
	
	public void initiateDocumentLoad();
	

}
