package com.suvikollc.resume_rag.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;

import com.suvikollc.resume_rag.service.ResumeChunkingService;

public class HybridChunkingOrchestrator implements ResumeChunkingService {

	private static final int CHARACTER_THRESHOLD = 7500;

	@Autowired
	private AgenticChunkingImpl agenticChunkingService;

	@Autowired
	private SectionBasedChunkingImpl sectionBasedChunkingImpl;

	@Autowired
	private SemanticChunkingImpl semanticChunkingService;

	@Override
	public List<Document> chunkResume(String resumeContent, String resumeFileName) {

		if (resumeContent.length() < CHARACTER_THRESHOLD) {
			return agenticChunkingService.chunkResume(resumeContent, resumeFileName);
		}

		List<Document> finalDocuments = new ArrayList<>();

		var allSections = sectionBasedChunkingImpl.extractSections(resumeContent);

		String experieceText = allSections.remove("experience");

		if (experieceText != null && !experieceText.isBlank()) {
			finalDocuments.addAll(semanticChunkingService.chunk(resumeContent, resumeFileName, "experience"));
		}

		String remainingText = allSections.values().stream().filter(text -> text != null && !text.isBlank())
				.collect(Collectors.joining("\n\n"));

		if (remainingText.length() < CHARACTER_THRESHOLD) {

			if (!remainingText.isBlank()) {
				finalDocuments.addAll(agenticChunkingService.chunkResume(remainingText, resumeFileName));

			}
		}

		else {
			String projectsText = allSections.remove("projects");
			if (projectsText != null && !projectsText.isBlank()) {
				finalDocuments.addAll(semanticChunkingService.chunk(projectsText, resumeFileName, "projects"));
			}

			String finalRemainingText = allSections.values().stream().filter(text -> text != null && !text.isBlank())
					.collect(Collectors.joining("\n\n"));

			if (!finalRemainingText.isBlank()) {

				finalDocuments.addAll(agenticChunkingService.chunkResume(finalRemainingText, resumeFileName));
			}

		}

		return finalDocuments;
	}

}
