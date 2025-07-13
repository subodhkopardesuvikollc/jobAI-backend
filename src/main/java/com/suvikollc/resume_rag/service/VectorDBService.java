package com.suvikollc.resume_rag.service;

import java.util.List;

import com.suvikollc.resume_rag.dto.SearchResultsDto;

public interface VectorDBService {
	
	public List<SearchResultsDto> getResults(String query);
	
	public void uploadToVectorDB(String fileName);
	
	public void initiateDocumentLoad();
	

}
