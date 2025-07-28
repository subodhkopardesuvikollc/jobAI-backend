package com.suvikollc.resume_rag.serviceImpl;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.suvikollc.resume_rag.dto.ResumeResultsDto;
import com.suvikollc.resume_rag.dto.SearchResultsDto;
import com.suvikollc.resume_rag.service.FileService;
import com.suvikollc.resume_rag.service.ResumeChunkingService;
import com.suvikollc.resume_rag.service.VectorDBService;

@Service
public class VectorDBServiceImpl implements VectorDBService {

	private static final Logger log = LoggerFactory.getLogger(VectorDBServiceImpl.class);

	@Autowired
	private VectorStore vectorStore;

	@Autowired
	private BlobServiceClient blobServiceClient;

	@Value("${azure.storage.resume.container.name}")
	private String resumeContainerName;

	@Autowired
	private KafkaProducer kafkaProducer;

	@Autowired
	private FileService fileService;

	@Autowired
	private ResumeChunkingService chunkingService;

	@Value("${azure.storage.jd.container.name}")
	private String jdContainerName;

	public void uploadToVectorDB(String fileName) {
		log.info("Processing document from file share: {}", fileName);
		try {

			var blobClient = fileService.getBlobClient(fileName, resumeContainerName);
			if (!blobClient.exists()) {
				log.error("File not found: {}", fileName);
				throw new RuntimeException("File not found: " + fileName);
			}

			InputStream fileInputStream = blobClient.openInputStream();
			var resource = new InputStreamResource(fileInputStream, fileName);
			var tikaReader = new TikaDocumentReader(resource);
			List<Document> documents = tikaReader.get();

//			TextSplitter textSplitter = new TokenTextSplitter();
//			List<Document> chunkedDocuments = textSplitter.apply(documents);
			String resumeContent = fileService.extractContent(documents);

			List<Document> chunkedDocuments = chunkingService.chunkResume(resumeContent, fileName);

			log.info("Split document {} into {} chunks.", fileName, chunkedDocuments.size());

			for (int i = 0; i < chunkedDocuments.size(); i++) {
				Document chunk = chunkedDocuments.get(i);
				String uniqueId = fileName + "-chunk-" + i;
				chunk.getMetadata().put("id", uniqueId);
				String sectionName = (String) chunk.getMetadata().get("section");
				chunk.getMetadata().put("section", sectionName.toLowerCase());

				chunk.getMetadata().put("source_file", fileName);
			}

			vectorStore.accept(chunkedDocuments);
			log.info("Successfully upserted document: {}", fileName);

		} catch (Exception e) {
			log.error("Failed to ingest document: " + fileName, e);
		}
	}

	private Map<String, Double> queryDocument(String jdBlobName) {

		try {

			var blobClient = fileService.getBlobClient(jdBlobName, jdContainerName);
			if (!blobClient.exists()) {
				log.error("JD file not found: {}", jdBlobName);
				throw new RuntimeException("JD file not found: " + jdBlobName);
			}

			try (InputStream inputStream = blobClient.openInputStream()) {

				String content = fileService.extractContent(inputStream);

				var searchConfig = SearchRequest.builder().query(content).topK(10).similarityThreshold(0.5).build();

				List<Document> similaritySearch = vectorStore.similaritySearch(searchConfig);
				Map<String, Double> resumes = similaritySearch.stream().collect(Collectors.toMap(doc -> {
					if (doc.getMetadata().get("source_file") == null) {
						log.warn("Document metadata does not contain 'source_file': {}", doc.getMetadata());
						return doc.getMetadata().get("source").toString();
					}
					return doc.getMetadata().get("source_file").toString();
				}, Document::getScore, (existing, newScore) -> Math.max(existing, newScore), LinkedHashMap::new));
				return resumes;
			}

		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return null;
	}

	public void initiateDocumentLoad() {

		log.info("Initiating document upload...");

		try {
			BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(resumeContainerName);
			var blobItems = blobContainerClient.listBlobs();

			for (BlobItem blobItem : blobItems) {

				String fileName = blobItem.getName();

				if (fileName != null) {

					kafkaProducer.sendFileEvent(fileName);

					log.info("File event created for file: " + fileName);
				}

			}
		} catch (Exception e) {
			log.error("Failed to initiate document upload: " + e.getMessage());
		}
	}

	public SearchResultsDto getResults(String jdBlobName) {

		Map<String, Double> resumes = queryDocument(jdBlobName);
		String jdUrl = fileService.getSharableUrl(jdBlobName, jdContainerName);

		try {
			if (resumes == null || resumes.isEmpty()) {
				log.info("No results found for query: {}", jdBlobName);
				return new SearchResultsDto(jdUrl, List.of());
			}

			var resumeResult = resumes.entrySet().stream().map(entry -> {
				String resumeName = entry.getKey();
				if (resumeName != null && !resumeName.isBlank()) {
				}
				Double score = entry.getValue();
				String resumeUrl = fileService.getSharableUrl(resumeName, resumeContainerName);
				return new ResumeResultsDto(resumeName, resumeUrl, score);
			}).collect(Collectors.toList());

			return new SearchResultsDto(jdUrl, resumeResult);

		} catch (Exception e) {
			log.error("Error processing results for query '{}': {}", jdBlobName, e.getMessage());
			throw new RuntimeException("Error processing results", e);
		}

	}

}
