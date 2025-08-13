package com.suvikollc.resume_rag.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suvikollc.resume_rag.dto.CommunicationDTO;
import com.suvikollc.resume_rag.service.CommunicationSerivce;

@RestController
@RequestMapping("/communication")
public class CommunicationController {

	@Autowired
	private CommunicationSerivce communicationSerivce;

	@PostMapping("/produce")
	public ResponseEntity<?> produceMessage(@RequestBody CommunicationDTO comDto) {

		communicationSerivce.produceCommunication(comDto);
		return ResponseEntity.ok("Message produced successfully");
	}

}
