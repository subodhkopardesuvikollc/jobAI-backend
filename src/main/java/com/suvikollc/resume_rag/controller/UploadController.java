package com.suvikollc.resume_rag.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.suvikollc.resume_rag.dto.SearchRequestDto;
import com.suvikollc.resume_rag.service.FileService;
import com.suvikollc.resume_rag.service.VectorDBService;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UploadController {

	@Autowired
	VectorDBService vectorDBService ;

	@Autowired
	FileService fileService;

	@PostMapping("/search")
	public ResponseEntity<?> getMethodName(@RequestBody SearchRequestDto dto) {

		if (dto == null || dto.getQuery() == null || dto.getQuery().isEmpty()) {
			return ResponseEntity.badRequest().body("Query cannot be null or empty");
		}

		return new ResponseEntity<>(vectorDBService.getResults(dto.getQuery()),
				org.springframework.http.HttpStatus.OK);

	}

	@PostMapping("/resume/upload")
	public ResponseEntity<?> uploadResume(@RequestParam MultipartFile file) {

		if (file == null || file.isEmpty()) {
			return ResponseEntity.badRequest().body("File cannot be null or empty");
		}

		try {
			var newFile = fileService.uploadFile(file);
			return ResponseEntity.ok("File: "+ newFile.toString() +" uploaded successfully");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Failed to upload file: " + e.getMessage());
		}

	}

	@PostMapping("/upload")
	public String initiateDocumentsLoad() {

		vectorDBService.initiateDocumentLoad();

		return "Initiated document upload";

	}
}
