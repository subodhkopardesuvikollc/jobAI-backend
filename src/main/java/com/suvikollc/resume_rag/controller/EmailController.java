package com.suvikollc.resume_rag.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suvikollc.resume_rag.dto.EmailDTO;
import com.suvikollc.resume_rag.service.EmailService;


@RestController
@RequestMapping("/email")
public class EmailController {

	@Autowired
	EmailService emailService;

	@GetMapping("/generate-email")
	public ResponseEntity<?> generateCustomReachOutEmail(@RequestParam String resumeBlobName,
			@RequestParam String jobTitle, @RequestParam String jdBlobName) {

		EmailDTO generatedEmail = emailService.generateCustomReachOutEmail(resumeBlobName, jobTitle, jdBlobName);
		return ResponseEntity.ok(generatedEmail);

	}
	
	@PostMapping("/send-email")
	public ResponseEntity<?> sendEmail(@RequestBody EmailDTO emailDTO) {
		emailService.sendEmail(emailDTO);
		return ResponseEntity.ok("Email sent successfully");
	}

}
