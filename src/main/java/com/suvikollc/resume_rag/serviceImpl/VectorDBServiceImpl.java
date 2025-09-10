package com.suvikollc.resume_rag.serviceImpl;

import java.io.FileNotFoundException;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.suvikollc.resume_rag.dto.ResumeResultsDto;
import com.suvikollc.resume_rag.dto.SearchResultsDto;
import com.suvikollc.resume_rag.entities.Resume.ResumeIndexStatus;
import com.suvikollc.resume_rag.service.FileService;
import com.suvikollc.resume_rag.service.ResumeChunkingService;
import com.suvikollc.resume_rag.service.ResumeService;
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
	private ResumeService resumeService;

	@Autowired
	@Qualifier("agenticChunkingImpl")
	private ResumeChunkingService agenticChunkingService;

	@Autowired
	@Qualifier("sectionBasedChunkingImpl")
	private ResumeChunkingService sectionBasedChunkingImpl;

	@Value("${azure.storage.jd.container.name}")
	private String jdContainerName;

	private static final int CHARACTER_THRESHOLD = 40000;

	public void uploadToVectorDB(String resumeFileName) {
		log.info("Processing document from file share: {}", resumeFileName);
		try {

			if (resumeService.isResumeIndexed(resumeFileName)) {
				log.info("Resume {} is already indexed. Skipping ingestion.", resumeFileName);
				return;
			}

			resumeService.updatedResumeIndexStatus(resumeFileName, ResumeIndexStatus.INDEXING);

			var blobClient = fileService.getBlobClient(resumeFileName, resumeContainerName);
			if (!blobClient.exists()) {
				log.error("File not found: {}", resumeFileName);
				throw new FileNotFoundException("File not found: " + resumeFileName);
			}

			try (InputStream fileInputStream = blobClient.openInputStream()) {

				var resource = new InputStreamResource(fileInputStream, resumeFileName);
				var tikaReader = new TikaDocumentReader(resource);
				List<Document> documents = tikaReader.get();

				String resumeContent = fileService.extractContent(documents);
				List<Document> chunkedDocuments;
				if (resumeContent.length() < CHARACTER_THRESHOLD) {
					chunkedDocuments = agenticChunkingService.chunkResume(resumeContent, resumeFileName);
				} else {
					chunkedDocuments = sectionBasedChunkingImpl.chunkResume(resumeContent, resumeFileName);
				}

				var contactDocument = chunkedDocuments.stream()
						.filter(doc -> "contact_info".equalsIgnoreCase((String) doc.getMetadata().get("section")))
						.findFirst();
				if (contactDocument.isEmpty()) {
					log.warn("No contact_info section found in resume: {}", resumeFileName);
				} else {
					resumeService.updateResumeContactInfo(resumeFileName, contactDocument.get().getText());
				}

				log.info("Split document {} into {} chunks.", resumeFileName, chunkedDocuments.size());

				for (int i = 0; i < chunkedDocuments.size(); i++) {
					Document chunk = chunkedDocuments.get(i);
					String uniqueId = resumeFileName + "-chunk-" + i;
					chunk.getMetadata().put("id", uniqueId);
					String sectionName = (String) chunk.getMetadata().get("section");
					chunk.getMetadata().put("section", sectionName.toLowerCase());

					chunk.getMetadata().put("source_file", resumeFileName);
				}

				vectorStore.accept(chunkedDocuments);
				log.info("Successfully upserted document: {}", resumeFileName);
				resumeService.updatedResumeIndexStatus(resumeFileName, ResumeIndexStatus.INDEXED);
			}

		} catch (FileNotFoundException fnfEx) {
			resumeService.updatedResumeIndexStatus(resumeFileName, ResumeIndexStatus.FAILED);
			throw new RuntimeException("File not found: " + resumeFileName, fnfEx);
		}

		catch (Exception e) {
			log.error("Failed to ingest document: " + resumeFileName, e);
			resumeService.updatedResumeIndexStatus(resumeFileName, ResumeIndexStatus.FAILED);
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
