package com.suvikollc.resume_rag.controller;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.azure.core.annotation.QueryParam;
import com.suvikollc.resume_rag.entities.Jd;
import com.suvikollc.resume_rag.entities.Resume;
import com.suvikollc.resume_rag.service.FileService;
import com.suvikollc.resume_rag.service.JDService;
import com.suvikollc.resume_rag.service.ResumeChunkingService;
import com.suvikollc.resume_rag.service.ResumeService;
import com.suvikollc.resume_rag.service.VectorDBService;

@RestController
public class UploadController {

	@Autowired
	VectorDBService vectorDBService;

	@Autowired
	FileService fileService;

	@Autowired
	ResumeChunkingService resumeChunkingService;

	@Autowired
	JDService jdService;

	@Autowired
	ResumeService resumeService;

	@PostMapping("/chunk")
	public List<Document> chunkResume() {
		String fileName = "Resume.Saivarun__.pdf";
		var blobClient = fileService.getBlobClient(fileName, "resumes");

		var InputStream = blobClient.openInputStream();
		String resumeContent = fileService.extractContent(InputStream);

		List<Document> chunkResume = resumeChunkingService.chunkResume(resumeContent, fileName);
		System.out.println("Number of chunks " + chunkResume.size());
		return chunkResume;

	}

	@GetMapping("/resume-retrieve")
	public ResponseEntity<?> getJdKeywords(@QueryParam(value = "") String jdBlobName) {
		String fileName = "Jani Syed Resume(Java Full Stack) (1).docx";

		try {
			String keywords = "Extracted Keywords: Frontend Engineer, development, implementation, testing, operational tooling, engineering teams, performant applications, operational excellence, productivity improvement, detail oriented, organizational skills, independent work, multitasking, professionalism, software engineering practices, front end applications, software and hardware systems interaction, design and implementation, complex systems, JavaScript, HTML, CSS, web development, mobile development, object-oriented design, data structures, algorithm design, problem solving, complexity analysis, React, Bachelor’s degree, Computer Science, Computer Engineering, AWS Certified\r\n"
					+ "";

			return ResponseEntity.ok(resumeService.retrieveRelavantCandidateWork(fileName, jdBlobName, keywords));
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Failed to extract keywords: " + e.getMessage());
		}
	}

	@PostMapping("/resume/upload")
	public ResponseEntity<?> uploadResume(@RequestParam MultipartFile file) {

		if (file == null || file.isEmpty()) {
			return ResponseEntity.badRequest().body("File cannot be null or empty");
		}

		try {
			var newFile = fileService.uploadFile(file, Resume.class);
			return ResponseEntity.ok("Resume: " + newFile.toString() + " uploaded successfully");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Failed to upload file: " + e.getMessage());
		}

	}

	@PostMapping("/jd/upload")
	public ResponseEntity<?> uploadJd(@RequestParam MultipartFile file) {

		if (file == null || file.isEmpty()) {
			return ResponseEntity.badRequest().body("File cannot be null or empty");
		}

		try {
			var newFile = fileService.uploadFile(file, Jd.class);
			return ResponseEntity.ok("Resume: " + newFile.toString() + " uploaded successfully");
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
