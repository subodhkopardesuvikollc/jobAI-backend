package com.suvikollc.resume_rag.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suvikollc.resume_rag.dto.InterviewQuestionsDTO;
import com.suvikollc.resume_rag.service.InterviewService;

@RestController
@RequestMapping("/interview")
public class InterviewController {

	@Autowired
	InterviewService interviewService;

	@PostMapping
	public ResponseEntity<?> updateInterviewQuestions(@RequestBody InterviewQuestionsDTO dto) {

		interviewService.saveInterviewQuestionsById(dto.getResumeId(), dto.getJdId(), dto.getQuestions());
		return ResponseEntity.ok("Interview questions saved successfully");

	}

	@GetMapping("/communications")
	public ResponseEntity<?> getInterviewCommunications(@RequestParam(required = true) String resumeId, 
			@RequestParam(required = true) String jdId) {

		var communications = interviewService.getInterviewCommunications(resumeId, jdId);
		return ResponseEntity.ok(communications);

	}

}
