package com.suvikollc.resume_rag.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.suvikollc.resume_rag.dto.SearchRequestDto;
import com.suvikollc.resume_rag.service.IngestionSerivce;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UploadController {

	@Autowired
	IngestionSerivce ingestionSerivce;

	@PostMapping("/search")
	public ResponseEntity<?> getMethodName(@RequestBody SearchRequestDto dto) {

		if (dto == null || dto.getQuery() == null || dto.getQuery().isEmpty()) {
			return ResponseEntity.badRequest().body("Query cannot be null or empty");
		}
		
		
		return new ResponseEntity<>(ingestionSerivce.getResults(dto.getQuery()), org.springframework.http.HttpStatus.OK);
		
		
		
	}

	@PostMapping("/upload")
	public String initiateDocumentsLoad() {

		ingestionSerivce.initiatingDocumentLoad();

		return "Initiated document upload";

	}
}
