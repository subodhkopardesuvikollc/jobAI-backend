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
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import com.azure.storage.file.share.ShareClient;
import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.ShareFileClient;
import com.azure.storage.file.share.ShareServiceClient;
import com.azure.storage.file.share.models.ShareFileItem;
import com.suvikollc.resume_rag.dto.SearchResultsDto;
import com.suvikollc.resume_rag.service.FileService;

@Service
public class VectorDBServiceImpl {

	private static final Logger log = LoggerFactory.getLogger(VectorDBServiceImpl.class);

	@Autowired
	private VectorStore vectorStore;

	@Autowired
	private ShareServiceClient shareServiceClient;

	@Value("${azure.storage.share.name}")
	private String shareName;

	@Autowired
	private KafkaProducer kafkaProducer;

	@Autowired
	private FileService fileService;

	public void uploadToVectorDB(String fileName) {
		log.info("Processing document from file share: {}", fileName);
		try {
			ShareClient shareClient = shareServiceClient.getShareClient(shareName);
			ShareDirectoryClient rootDirClient = shareClient.getRootDirectoryClient();
			ShareFileClient fileClient = rootDirClient.getFileClient(fileName);

			if (!fileClient.exists()) {
				log.error("File not found during processing: {}", fileName);
				throw new RuntimeException("File not found: " + fileName);
			}

			InputStream fileInputStream = fileClient.openInputStream();
			var resource = new InputStreamResource(fileInputStream, fileName);
			var tikaReader = new TikaDocumentReader(resource);
			List<Document> documents = tikaReader.get();

			TextSplitter textSplitter = new TokenTextSplitter();
			List<Document> chunkedDocuments = textSplitter.apply(documents);
			log.info("Split document {} into {} chunks.", fileName, chunkedDocuments.size());

			for (int i = 0; i < chunkedDocuments.size(); i++) {
				Document chunk = chunkedDocuments.get(i);
				String uniqueId = fileName + "-chunk-" + i;
				chunk.getMetadata().put("id", uniqueId);
				chunk.getMetadata().put("source_file", fileName);
			}

			vectorStore.accept(chunkedDocuments);
			log.info("Successfully upserted document: {}", fileName);

		} catch (Exception e) {
			log.error("Failed to ingest document: " + fileName, e);
		}
	}

	private Map<String, Double> queryDocument(String query) {

		try {
			var searchConfig = SearchRequest.builder().query(query).topK(10).similarityThreshold(0.5).build();

			List<Document> similaritySearch = vectorStore.similaritySearch(searchConfig);
			Map<String, Double> fileNames = similaritySearch.stream().collect(Collectors.toMap(doc -> {
				if (doc.getMetadata().get("source_file") == null) {
					log.warn("Document metadata does not contain 'source_file': {}", doc.getMetadata());
					return doc.getMetadata().get("source").toString();
				}
				return doc.getMetadata().get("source_file").toString();
			}, Document::getScore, (existing, newScore) -> Math.max(existing, newScore), LinkedHashMap::new));
			return fileNames;

		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return null;
	}

	public void initiateDocumentLoad() {

		log.info("Initiating document upload...");

		try {
			var shareClient = shareServiceClient.getShareClient(shareName);
			var rootDirClient = shareClient.getRootDirectoryClient();

			for (ShareFileItem fileItem : rootDirClient.listFilesAndDirectories()) {

				String fileName = fileItem.getName();

				if (fileName != null) {

					kafkaProducer.sendFileEvent(fileName);

					log.info("File event created for file: " + fileName);
				}

			}
		} catch (Exception e) {
			log.error("Failed to initiate document upload: " + e.getMessage());
		}
	}

	public List<SearchResultsDto> getResults(String query) {

		Map<String, Double> results = queryDocument(query);

		try {
			if (results == null || results.isEmpty()) {
				log.info("No results found for query: {}", query);
				return List.of();
			}

			return results.entrySet().stream().map(entry -> {
				String fileName = entry.getKey();
				Double score = entry.getValue();
				String fileUrl = fileService.getSharableUrl(fileName);
				return new SearchResultsDto(fileName, fileUrl, score);
			}).collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Error processing results for query '{}': {}", query, e.getMessage());
			throw new RuntimeException("Error processing results", e);
		}

	}

}
