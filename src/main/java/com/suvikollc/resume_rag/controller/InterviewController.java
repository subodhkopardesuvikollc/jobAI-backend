package com.suvikollc.resume_rag.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.suvikollc.resume_rag.dto.InterviewQuestionsDTO;
import com.suvikollc.resume_rag.service.InterviewService;

@RestController
public class InterviewController {

	@Autowired
	InterviewService interviewService;

	@PostMapping("/interview")
	public ResponseEntity<?> updateInterviewQuestions(@RequestBody InterviewQuestionsDTO dto) {

		interviewService.saveScreeningQuestionsById(dto.getResumeId(), dto.getJdId(), dto.getQuestions());
		return ResponseEntity.ok("Interview questions saved successfully");

	}

}
