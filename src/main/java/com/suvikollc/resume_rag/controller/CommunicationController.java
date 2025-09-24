package com.suvikollc.resume_rag.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suvikollc.resume_rag.dto.CommunicationDTO;
import com.suvikollc.resume_rag.exceptions.CallInProgressException;
import com.suvikollc.resume_rag.service.CallManagementService;
import com.suvikollc.resume_rag.service.CommunicationSerivce;
import com.suvikollc.resume_rag.service.WhatsAppService;

@RestController
@RequestMapping("/communication")
public class CommunicationController {

	Logger log = LoggerFactory.getLogger(CommunicationController.class);

	@Autowired
	private CommunicationSerivce communicationSerivce;

	@Autowired
	private CallManagementService callManagementService;

	@Autowired
	private WhatsAppService whatsAppService;

	@PostMapping("/produce")
	public ResponseEntity<?> produceMessage(@RequestBody CommunicationDTO comDto) {

		communicationSerivce.produceCommunication(comDto);

		return ResponseEntity.ok("Message produced successfully");
	}

	@PostMapping("/call/produce")
	public ResponseEntity<?> produceCallMessage(@RequestBody CommunicationDTO comDto) {
		try {
			String response = callManagementService.initiateCall(comDto);
			return ResponseEntity.ok(response);
		} catch (CallInProgressException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (Exception e) {
			log.error("Error producing message: {}", e.getMessage(), e);
			return ResponseEntity.status(500).body("Failed to produce message: " + e.getMessage());
		}

	}

//	@PostMapping("/call/start")
//	public ResponseEntity<?> startCall(@RequestBody CallInitiationDTO callDto) {
//
//		callService.startCall(callDto);
//		return ResponseEntity.ok("Call started successfully");
//	}

	@PostMapping("/whatsapp/send")
	public ResponseEntity<?> sendWhatsAppMessage(@RequestParam(required = true) String resumeId,
			@RequestParam(required = true) String jdId) {

		try {

			return ResponseEntity.ok(whatsAppService.sendMessage(resumeId, jdId));

		} catch (Exception e) {
			log.error("Error sending WhatsApp message: {}", e.getMessage(), e);
			return ResponseEntity.status(500).body("Failed to send WhatsApp message: " + e.getMessage());
		}

	}

	@PostMapping("/call/callback")
	public ResponseEntity<String> handleCallback(@RequestBody(required = false) String payload) {
		// TODO: Should initiate screening analysis upon call end event
		log.info("Received ACS callback: {}", payload);
		return ResponseEntity.ok().build();
	}

}
