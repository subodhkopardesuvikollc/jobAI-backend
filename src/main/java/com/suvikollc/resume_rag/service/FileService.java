package com.suvikollc.resume_rag.service;

import org.springframework.web.multipart.MultipartFile;

import com.suvikollc.resume_rag.entities.File;

public interface FileService {

	public <T extends File> T uploadFile(MultipartFile file, Class<T> fileType);

	public String getSharableUrl(String fileName);

}
