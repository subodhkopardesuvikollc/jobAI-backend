package com.suvikollc.resume_rag.serviceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import com.suvikollc.resume_rag.service.ResumeChunkingService;

@Service
public class ResumeChunkingServiceImpl implements ResumeChunkingService {

	private static final String[] SECTION_HEADERS = { "Summary", "Professional Summary", "About Me", "Experience",
			"Employment History", "Professional Experience", "Work Experience", "Education", "Education Details",
			"Skills", "Technical Skills", "Relevant Skills", "Projects", "Project Experience", "Projects Overview",
			"Portfolio", "Awards", "Certifications", "Publications", "Interests", "Volunteer Experience" };

	private static final Pattern SECTION_PATTERN;
	static {
		StringBuilder patternBuilder = new StringBuilder();
		for (int i = 0; i < SECTION_HEADERS.length; i++) {
			patternBuilder.append("(?i)^\\s*").append(Pattern.quote(SECTION_HEADERS[i])).append("(:?)\\s*$");
			if (i < SECTION_HEADERS.length - 1) {
				patternBuilder.append("|");
			}
		}
		SECTION_PATTERN = Pattern.compile(patternBuilder.toString(), Pattern.MULTILINE);
	}

	@Override
	public List<Document> chunkResume(String resumeContent, String resumeFileName) {
		List<Document> documents = new ArrayList<>();
		Matcher matcher = SECTION_PATTERN.matcher(resumeContent);

		int lastEnd = 0;
		int sectionIndex = 0;
		String currentSectionTitle = "Uncategorized";

		while (matcher.find()) {
			String foundHeader = matcher.group();
			int start = matcher.start();

			if (start > lastEnd) {
				String sectionContent = resumeContent.substring(lastEnd, start).trim();
				if (!sectionContent.isEmpty()) {
					addSectionContentAsDocuments(documents, sectionContent, currentSectionTitle, resumeFileName,
							sectionIndex++);
				}
			}

			currentSectionTitle = foundHeader.trim().replaceAll("(?i):?$", "").trim();
			lastEnd = matcher.end();
		}

		if (lastEnd < resumeContent.length()) {
			String content = resumeContent.substring(lastEnd).trim();
			if (!content.isEmpty()) {
				addSectionContentAsDocuments(documents, content, currentSectionTitle, resumeFileName, sectionIndex++);
			}
		}
		return documents;
	}

	private void addSectionContentAsDocuments(List<Document> documents, String sectionContent,
			String currentSectionTitle, String resumeFileName, int baseSectionIndex) {
		final int MAX_CHARACTERS_PER_CHUNK = 8000;

		if (sectionContent.length() > MAX_CHARACTERS_PER_CHUNK) {
			List<String> subChunks = chunkTextFixedSize(sectionContent, MAX_CHARACTERS_PER_CHUNK, 200);
			for (int i = 0; i < subChunks.size(); i++) {
				Map<String, Object> metadata = new HashMap<>();
				metadata.put("section", currentSectionTitle);
				metadata.put("originalSectionIndex", baseSectionIndex);
				metadata.put("subChunkIndex", i);
				metadata.put("chunkSize", subChunks.get(i).length());
				documents.add(new Document(subChunks.get(i), metadata));
			}
		} else {
			Map<String, Object> metadata = new HashMap<>();
			metadata.put("section", currentSectionTitle);
			metadata.put("originalSectionIndex", baseSectionIndex);
			metadata.put("chunkSize", sectionContent.length());
			documents.add(new Document(sectionContent, metadata));
		}

	}

	private List<String> chunkTextFixedSize(String text, int chunkSize, int overlap) {

		List<String> chunks = new ArrayList<>();
		int currentPosition = 0;

		while (currentPosition < text.length()) {
			int endPosition = Math.min(currentPosition + chunkSize, text.length());
			String chunk = text.substring(currentPosition, endPosition);
			chunks.add(chunk);

			if (endPosition == text.length()) {
				break;
			}
			currentPosition += (chunkSize - overlap);
			if (currentPosition < 0) {
				currentPosition = 0;
			}
		}
		return chunks;
	}

}
