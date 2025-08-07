package com.suvikollc.resume_rag.serviceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SemanticChunkingImpl {

	@Autowired
	private EmbeddingModel embeddingModel;

	private static final double SIMILARITY_THRESHOLD = 0.7;

	private static final int CUMULATIVE_OVERLAP_SENTENCE_COUNT = 1;

	public List<Document> chunk(String textBlock, String fileName, String sectionName) {

		String normalizedText = normalizeTextForSentenceSplitting(textBlock);

		String[] sentences = normalizedText.split("(?<=[.!?])\\s+");

		if (sentences.length <= 1) {
			return List.of(new Document(textBlock, createMetadata(fileName, sectionName)));

		}

		EmbeddingRequest embeddingRequest = new EmbeddingRequest(List.of(sentences),
				AzureOpenAiEmbeddingOptions.builder().build());

		EmbeddingResponse embeddingResponse = embeddingModel.call(embeddingRequest);
		List<Embedding> embeddings = embeddingResponse.getResults();

		List<Integer> breakPoints = new ArrayList<>();
		for (int i = 0; i < embeddings.size() - 1; i++) {
			double similarity = calculateCosineSimilarity(embeddings.get(i).getOutput(),
					embeddings.get(i + 1).getOutput());
			if (similarity < SIMILARITY_THRESHOLD) {
				breakPoints.add(i + 1);
			}
		}

		List<String> textChunks = new ArrayList<>();
		int startIdx = 0;
		for (int bp : breakPoints) {
			String mainChunkContent = String.join(" ", Arrays.copyOfRange(sentences, startIdx, bp));
			textChunks.add(mainChunkContent);
			startIdx = Math.max(0, bp - CUMULATIVE_OVERLAP_SENTENCE_COUNT);
		}
		textChunks.add(String.join(" ", Arrays.copyOfRange(sentences, startIdx, sentences.length)));

		return IntStream.range(0, textChunks.size()).mapToObj(i -> {
			Map<String, Object> metadata = createMetadata(fileName, sectionName);
			metadata.put("chunk_sequence", i + 1);
			return new Document(textChunks.get(i), metadata);
		}).collect(Collectors.toList());
	}

	private double calculateCosineSimilarity(float[] v1, float[] v2) {
		if (v1.length != v2.length) {
			throw new IllegalArgumentException("Vectors must have the same dimension");
		}
		double dotProduct = 0.0;
		double normA = 0.0;
		double normB = 0.0;
		for (int i = 0; i < v1.length; i++) {
			dotProduct += v1[i] * v2[i];
			normA += Math.pow(v1[i], 2);
			normB += Math.pow(v2[i], 2);
		}
		if (normA == 0 || normB == 0) {
			return 0.0;
		}
		return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	}

	private String normalizeTextForSentenceSplitting(String rawText) {
		// Replace multiple newlines/tabs with a single space
		String cleaned = rawText.replaceAll("[\\n\\t]+", " ");
		// Ensure there's a space after periods for the regex splitter to work correctly
		cleaned = cleaned.replaceAll("\\.(?!\\s)", ". ");
		// Replace multiple spaces with a single space
		return cleaned.replaceAll("\\s{2,}", " ").trim();
	}

	private Map<String, Object> createMetadata(String fileName, String sectionName) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("source_file", fileName);
		metadata.put("section", sectionName);
		metadata.put("source", fileName);
		metadata.put("chunking_method", "semantic_cumulative");
		return metadata;
	}

}
