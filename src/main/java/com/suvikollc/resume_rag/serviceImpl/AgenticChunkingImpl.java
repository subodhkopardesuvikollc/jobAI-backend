package com.suvikollc.resume_rag.serviceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.suvikollc.resume_rag.dto.ResumeChunkDTO;
import com.suvikollc.resume_rag.service.ResumeChunkingService;

@Service
public class AgenticChunkingImpl implements ResumeChunkingService {

	private Logger logger = LoggerFactory.getLogger(AgenticChunkingImpl.class);

	@Autowired
	private ChatClient chatClient;

	private static final String SYSTEM_PROMPT = """
			You are an expert HR document parser. Your task is to analyze the following resume text and deconstruct it into a structured JSON array of objects, where each object is a chunk.

			The required JSON format for each chunk is: {"type": "...", "content": "..."}

			## Core Directives and Constraints
			1.  Ensure the 'content' for each chunk is the original, unmodified text from the resume.
			2.  **Experience & Project Allocation:** You have a maximum budget of **15 chunks** for 'experience' and 'project' types combined. Your primary goal is to preserve each individual job experience and project as a separate chunk within this limit.
			3.  **Targeted Merging for Priority Sections:** If, and only if, the total count of individual 'experience' and 'project' entries exceeds 15, you MUST merge the chronologically oldest or least detailed entries to fit within the 15-chunk budget. Do not merge recent, detailed roles unless absolutely necessary.
			4.  **Aggressive Consolidation for Other Sections:** For all other types, you must be aggressive in consolidation. Always aim to create only ONE chunk for each of the following types: 'skills', 'education', 'certification', 'publication'. The 'contact_info' and 'summary' should also each be a single chunk.
			5.  **Output Format:** Return ONLY the raw JSON array. Do not include any explanations, comments, or markdown code fences.

			## Possible 'type' values for JSON:
			- "contact_info": Contains email, phone, address, and profile links.
			- "summary": The professional summary or objective statement.
			- "skills": A single, combined block of ALL technical and soft skills.
			- "experience": A single, complete work experience entry OR a merged block of older experiences.
			- "education": A single, combined block containing ALL educational qualifications.
			- "project": A single project description.
			- "certification": A single, combined block of ALL certifications.
			- "publication": A single, combined block of ALL publications.
			""";

	@Override
	public List<Document> chunkResume(String resumeContent, String resumeFileName) {

		logger.info("Starting agentic chunking for document ID: {} to produce Document objects", resumeFileName);

		try {
			List<ResumeChunkDTO> intermediateChunks = chatClient.prompt().system(SYSTEM_PROMPT).user(resumeContent)
					.call().entity(new ParameterizedTypeReference<List<ResumeChunkDTO>>() {
					});
			logger.info("LLM returned {} structured chunks. Now transforming to Document objects.",
					intermediateChunks.size());

			var finalChunks = intermediateChunks.stream().map(chunk -> {
				Map<String, Object> metadata = new HashMap<>();
				metadata.put("source_file", resumeFileName);
				metadata.put("section", chunk.type());
				metadata.put("source", resumeFileName);

				metadata.put("chunking_method", "agentic");

				return new Document(chunk.content(), metadata);

			}).collect(Collectors.toList());

			return finalChunks;

		} catch (Exception e) {
			logger.error("Error during agentic chunking for document ID: {}: {}", resumeFileName, e.getMessage(), e);
			throw new RuntimeException("Failed to process resume '" + resumeFileName + "' with AI.", e);
		}

	}

}
