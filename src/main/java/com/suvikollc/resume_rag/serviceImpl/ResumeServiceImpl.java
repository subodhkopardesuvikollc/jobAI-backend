package com.suvikollc.resume_rag.serviceImpl;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.suvikollc.resume_rag.service.ResumeService;

@Service
public class ResumeServiceImpl implements ResumeService {

	@Autowired
	private VectorStore vectorStore;

	@Override
	public List<Document> retrieveRelavantCandidateWork(String resumeBlobName, String jobTitle, String jdKeywords) {

		String queryText = "Candidate's most impactful achievements and contributions relevant to a " + jobTitle
				+ " role, focusing on: " + jdKeywords;
		FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();

		Expression resumeAndSectionFilter = filterBuilder.and(filterBuilder.eq("source_file", resumeBlobName),
				filterBuilder.in("section", "experience", "professional experience", "projects", "project experience",
						"projects overview", "portfolio", "employment history"))
				.build();

		try {
			SearchRequest request = SearchRequest.builder().query(queryText).filterExpression(resumeAndSectionFilter)
					.topK(3).build();
			List<Document> retrievedDocuments = vectorStore.similaritySearch(request);
			return retrievedDocuments;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Failed to retrieve relevant candidate work: " + e.getMessage());
		}
	}

}
