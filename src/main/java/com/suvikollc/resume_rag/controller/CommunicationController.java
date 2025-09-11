package com.suvikollc.resume_rag.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suvikollc.resume_rag.dto.CommunicationDTO;
import com.suvikollc.resume_rag.service.CallService;
import com.suvikollc.resume_rag.service.CommunicationSerivce;

@RestController
@RequestMapping("/communication")
public class CommunicationController {

	Logger log = LoggerFactory.getLogger(CommunicationController.class);

	@Autowired
	private CommunicationSerivce communicationSerivce;

	@Autowired
	private CallService callService;

	@PostMapping("/produce")
	public ResponseEntity<?> produceMessage(@RequestBody CommunicationDTO comDto) {

		communicationSerivce.produceCommunication(comDto);
		return ResponseEntity.ok("Message produced successfully");
	}

	@PostMapping("/call/start")
	public ResponseEntity<?> startCall(@RequestBody String resumeId) {

		callService.startCall(resumeId);
		return ResponseEntity.ok("Call started successfully");
	}

	@PostMapping("/call/callback")
	public ResponseEntity<String> handleCallback(@RequestBody(required = false) String payload) {

		// Normal event handling
		System.out.println("Received ACS callback: " + payload);
		return ResponseEntity.ok().build();
	}

}
