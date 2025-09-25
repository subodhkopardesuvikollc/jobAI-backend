package com.suvikollc.resume_rag.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suvikollc.resume_rag.dto.CommunicationDTO;
import com.suvikollc.resume_rag.dto.WhatsAppWebhook;
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

	@Value("${whatsapp.verify.token}")
	private String whatsappVerifyToken;

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

	@PostMapping("/whatsapp/callback")
	public ResponseEntity<?> handleWhatsAppCallback(@RequestBody(required = false) WhatsAppWebhook payload) {

		if (payload != null && payload.entry() != null) {
			log.info("Received WhatsApp callback: {}", payload);
			String waId = payload.entry().get(0).changes().get(0).value().contacts().get(0).waId();
			String message = payload.entry().get(0).changes().get(0).value().messages().get(0).text().body();
			if(waId != null) {
				whatsAppService.replyToMessage(waId, message);
			} 
			
		} else {
			log.warn("Received empty or invalid WhatsApp callback payload.");
		}
		return ResponseEntity.ok().build();
	}

	@GetMapping("/whatsapp/callback")
	public ResponseEntity<?> verifyWhatsAppCallBack(@RequestParam("hub.mode") String mode,
			@RequestParam("hub.challenge") String challenge, @RequestParam("hub.verify_token") String token) {

		if ("subscribe".equals(mode) && whatsappVerifyToken.equals(token)) {
			log.info("WhatsApp webhook verified successfully.");
			return ResponseEntity.ok(challenge);
		}
		return ResponseEntity.ok().build();
	}

}
