package com.suvikollc.resume_rag.controller;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.azure.core.annotation.QueryParam;
import com.suvikollc.resume_rag.entities.Jd;
import com.suvikollc.resume_rag.entities.Resume;
import com.suvikollc.resume_rag.service.EmailService;
import com.suvikollc.resume_rag.service.FileService;
import com.suvikollc.resume_rag.service.JDService;
import com.suvikollc.resume_rag.service.ResumeChunkingService;
import com.suvikollc.resume_rag.service.ResumeService;
import com.suvikollc.resume_rag.service.VectorDBService;
import com.suvikollc.resume_rag.serviceImpl.SectionBasedChunkingImpl;
import com.suvikollc.resume_rag.serviceImpl.SemanticChunkingImpl;

@RestController
public class UploadController {

	@Autowired
	VectorDBService vectorDBService;

	@Autowired
	FileService fileService;

	@Autowired
	@Qualifier("sectionBasedChunkingImpl")
	ResumeChunkingService resumeChunkingService;

	@Autowired
	SemanticChunkingImpl semanticChunkingService;

	@Autowired
	SectionBasedChunkingImpl impl;

	@Autowired
	JDService jdService;

	@Autowired
	ResumeService resumeService;

	@Autowired
	EmailService emailService;

	@GetMapping("/chunk")
	public List<Document> chunkResume(@RequestParam String fileName) {
		var blobClient = fileService.getBlobClient(fileName, "resumes");

		try (var InputStream = blobClient.openInputStream()) {

			String resumeContent = fileService.extractContent(InputStream);
			List<Document> chunkResume = resumeChunkingService.chunkResume(resumeContent, fileName);
			System.out.println("Number of chunks " + chunkResume.size());
			return chunkResume;
		}

	}

	@GetMapping("/extract-sections")
	public ResponseEntity<?> extractSections(@RequestParam String fileName) {
		var blobClient = fileService.getBlobClient(fileName, "resumes");

		try (var InputStream = blobClient.openInputStream()) {

			String resumeContent = fileService.extractContent(InputStream);
			Map<String, String> sections = impl.extractSections(resumeContent);

			var experienceText = sections.remove("projects");

			return ResponseEntity.ok(semanticChunkingService.chunk(experienceText, fileName, "experience"));
		}

	}

//	@GetMapping("/resume-retrieve")
//	public ResponseEntity<?> getJdKeywords(@QueryParam(value = "") String jdBlobName,
//			@QueryParam(value = "") String resumeBlobName) {
//		System.out.println("jdBlobName: " + jdBlobName);
//		System.out.println(jdBlobName.split("\\.")[0]);
//
//		String jobTitle = jdBlobName.split("\\.")[0];
//
////		var response = emailService.generateCustomReachOutEmail(resumeBlobName, jobTitle, jdBlobName);
//		var response = resumeService.retrieveRelavantCandidateWork(resumeBlobName, jobTitle,
//				jdService.generateKeywords(jdBlobName));
//		if (response == null) {
//			return ResponseEntity.badRequest().body("Failed to generate email");
//		}
//		return ResponseEntity.ok(response);
//
//	}

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
