package com.suvikollc.resume_rag.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.QueueMessageItem;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.suvikollc.resume_rag.dto.CallInitiationDTO;
import com.suvikollc.resume_rag.dto.CommunicationDTO;
import com.suvikollc.resume_rag.dto.CommunicationDTO.CommunicationType;
import com.suvikollc.resume_rag.exceptions.CallInProgressException;
import com.suvikollc.resume_rag.service.CallService;

import jakarta.annotation.PostConstruct;

@Service
public class CallConsumer {

	private QueueClient phoneQueueClient;

	@Autowired
	private CallService callService;

	Logger log = LoggerFactory.getLogger(CallConsumer.class);

	private ObjectMapper objectMapper;

	@Value("${azure.storage.connection-string}")
	private String connectionString;

	@PostConstruct
	public void init() {

		this.phoneQueueClient = new QueueClientBuilder().connectionString(connectionString)
				.queueName(CommunicationType.PHONE.getQueueName()).buildClient();
		this.objectMapper = new ObjectMapper().findAndRegisterModules()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).registerModule(new JavaTimeModule());
	}

	@Scheduled(fixedDelay = 30000)
	public void checkForNewCalls() {
		log.info("Checking for new call requests in the queue...");

		for (QueueMessageItem message : phoneQueueClient.receiveMessages(1)) {
			try {
				log.info("Received call request message: {}", message.getBody().toString());
				CommunicationDTO callDTO = objectMapper.readValue(message.getBody().toString(), CommunicationDTO.class);
				CallInitiationDTO callInitDTO = objectMapper.convertValue(callDTO.getPayload(),
						CallInitiationDTO.class);
				callService.startCall(callInitDTO);
				phoneQueueClient.deleteMessage(message.getMessageId(), message.getPopReceipt());
				log.info("Processed and deleted call request message: {}", message.getMessageId());
			} catch (CallInProgressException e) {
				phoneQueueClient.deleteMessage(message.getMessageId(), message.getPopReceipt());
				log.warn("Call already in progress. Discarding message: {}", message.getMessageId());

			} catch (Exception e) {
				log.error("Error processing call request message: {}", message.getMessageId(), e);
			}
		}
	}

}
