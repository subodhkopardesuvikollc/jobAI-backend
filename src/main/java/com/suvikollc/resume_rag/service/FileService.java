package com.suvikollc.resume_rag.service;

import java.io.InputStream;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import com.azure.storage.blob.BlobClient;
import com.suvikollc.resume_rag.dto.FileDTO;
import com.suvikollc.resume_rag.entities.File;

public interface FileService {

	public <T extends File> T uploadFile(MultipartFile file, Class<T> fileType);

	public String getSharableUrl(String blobName, String containerName);

	public BlobClient getBlobClient(String blobName, String containerName);

	public String extractContent(InputStream stream);

	public String extractContent(List<Document> documents);
	
	public <T extends File> T getFileByFileName(String fileName, Class<T> fileType);

	public <T extends File> List<T> getAllFiles(Class<T> fileType);

	<T extends File> List<FileDTO<T>> getAllFilesWithUrl(Class<T> fileType);

	<T extends File> Page<FileDTO<T>> getAllFilesWithUrl(Class<T> fileType, Integer page, Integer size);

}
