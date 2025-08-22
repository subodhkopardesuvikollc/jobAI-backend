package com.suvikollc.resume_rag.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suvikollc.resume_rag.dto.ResumeAnalysisResponseDTO;
import com.suvikollc.resume_rag.service.ResumeService;

@RestController
@RequestMapping("/resume")
public class ResumeController {

	@Autowired
	private ResumeService resumeService;

	@GetMapping("/analyze")
	public ResponseEntity<?> analyzeResume(@RequestParam String resumeBlobName, @RequestParam String jdBlobName) {
		ResumeAnalysisResponseDTO response = resumeService.analyzeResume(resumeBlobName, jdBlobName);
		return ResponseEntity.ok(response);
	}

}
