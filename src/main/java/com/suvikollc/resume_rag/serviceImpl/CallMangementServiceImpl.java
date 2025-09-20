package com.suvikollc.resume_rag.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvikollc.resume_rag.dto.CallInitiationDTO;
import com.suvikollc.resume_rag.dto.CommunicationDTO;
import com.suvikollc.resume_rag.entities.Interview.Status;
import com.suvikollc.resume_rag.exceptions.CallInProgressException;
import com.suvikollc.resume_rag.service.CallManagementService;
import com.suvikollc.resume_rag.service.CallService;
import com.suvikollc.resume_rag.service.CommunicationSerivce;
import com.suvikollc.resume_rag.service.InterviewService;

@Service
public class CallMangementServiceImpl implements CallManagementService {

	Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private CommunicationSerivce commService;

	@Autowired
	private CallService callService;

	@Autowired
	private InterviewService interviewService;

	private ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String initiateCall(CommunicationDTO commDto) {

		try {

			CallInitiationDTO callDto = objectMapper.convertValue(commDto.getPayload(), CallInitiationDTO.class);
			if (callService.isCallInProgress()) {
				interviewService.updateInterviewStatus(Status.FAILED, callDto.getResumeId(), callDto.getJdId());
				throw new CallInProgressException("A call is already in progress. Please try again later.");
			}
			interviewService.updateInterviewStatus(Status.QUEUED, callDto.getResumeId(), callDto.getJdId());
			commService.produceCommunication(commDto);
		} catch (Exception e) {

			log.error("Error producing call: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to produce call: " + e.getMessage());
		}

		return "Call initiation message sent successfully";

	}

}
