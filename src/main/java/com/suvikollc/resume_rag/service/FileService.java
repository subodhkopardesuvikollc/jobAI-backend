package com.suvikollc.resume_rag.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import com.suvikollc.resume_rag.dto.FileDTO;
import com.suvikollc.resume_rag.entities.File;

public interface FileService {

	public <T extends File> T uploadFile(MultipartFile file, Class<T> fileType);

	public String getSharableUrl(String blobName, String containerName);
	
	public <T extends File> List<T> getAllFiles(Class<T> fileType);

	<T extends File> List<FileDTO<T>> getAllFilesWithUrl(Class<T> fileType);
	
	<T extends File> Page<FileDTO<T>> getAllFilesWithUrl(Class<T> fileType, Integer page, Integer size);

}
