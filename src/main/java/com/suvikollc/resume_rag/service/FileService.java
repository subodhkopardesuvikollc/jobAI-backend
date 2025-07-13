package com.suvikollc.resume_rag.service;

import org.springframework.web.multipart.MultipartFile;

import com.suvikollc.resume_rag.entities.File;

public interface FileService {

	public File uploadFile(MultipartFile file);

	public String getSharableUrl(String fileName);

}
