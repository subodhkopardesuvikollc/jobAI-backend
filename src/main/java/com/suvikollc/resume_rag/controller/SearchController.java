package com.suvikollc.resume_rag.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suvikollc.resume_rag.dto.SearchRequestDto;
import com.suvikollc.resume_rag.entities.Jd;
import com.suvikollc.resume_rag.entities.Resume;
import com.suvikollc.resume_rag.service.FileService;
import com.suvikollc.resume_rag.service.VectorDBService;

@RestController
public class SearchController {

	@Autowired
	VectorDBService vectorDBService;

	@Autowired
	FileService fileService;

	@PostMapping("/search")
	public ResponseEntity<?> getMethodName(@RequestBody SearchRequestDto dto) {

		if (dto == null || dto.getBlobName() == null || dto.getBlobName().isEmpty()) {
			return ResponseEntity.badRequest().body("Query cannot be null or empty");
		}

		return new ResponseEntity<>(vectorDBService.getResults(dto.getBlobName()),
				org.springframework.http.HttpStatus.OK);

	}

	@GetMapping("/jd")
	public ResponseEntity<?> getAllJds() {
		return ResponseEntity.ok(fileService.getAllFiles(Jd.class));
	}

	@GetMapping("/resume/{fileName}")
	public ResponseEntity<?> getResumeByFileName(@PathVariable String fileName) {
		Resume resume = fileService.getFileByFileName(fileName, Resume.class);
		if (resume == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(resume);
	}

	@GetMapping("/jd/{fileName}")
	public ResponseEntity<?> getJdByFileName(@PathVariable String fileName) {
		Jd jd = fileService.getFileByFileName(fileName, Jd.class);
		if (jd == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(jd);
	}

	@GetMapping("/resume")
	public ResponseEntity<?> getAllResumes(@RequestParam(required = false) Integer pageNo,
			@RequestParam(required = false) Integer pageSize) {
		if (pageNo != null && pageSize != null) {
			return ResponseEntity.ok(fileService.getAllFilesWithUrl(Resume.class, pageNo, pageSize));
		}

		return ResponseEntity.ok(fileService.getAllFilesWithUrl(Resume.class));
	}

}
