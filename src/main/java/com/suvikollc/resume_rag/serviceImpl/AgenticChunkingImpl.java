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
			You are an expert HR document parser. Your task is to analyze the following resume text and deconstruct it into a structured JSON array.
			Each object in the array must represent a single, logically complete chunk of information.

			The required JSON format for each chunk is: {"type": "...", "content": "..."}

			Possible 'type' values are:
			- "contact_info": A block containing email, phone, address, and profile links (LinkedIn, GitHub).
			- "summary": The professional summary or objective statement.
			- "skills": A self-contained block listing technical or soft skills.
			- "experience": A single, complete work experience entry. It MUST include company, title, dates, and all related responsibilities/achievements for that specific role.
			- "education": A single, combined block containing ALL educational qualifications from the resume.
			- "project": A single project description.
			- "certification": A single certification, license, or credential.
			- "publication": A single published work.

			 Rules:
			 1.  Ensure the 'content' for each chunk is the original, unmodified text from the resume.
			 2.  Combine all distinct educational entries (degrees, diplomas) into one 'education' chunk.
			 3.  Discard any miscellaneous sections that do not fit the categories above (e.g., hobbies, references). Do not create a chunk with the type "other".
			 4.  Return ONLY the raw JSON array, with no surrounding text, explanations, or markdown code fences.
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
