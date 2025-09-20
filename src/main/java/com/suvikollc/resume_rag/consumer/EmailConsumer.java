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
import com.suvikollc.resume_rag.dto.CommunicationDTO;
import com.suvikollc.resume_rag.dto.CommunicationDTO.CommunicationType;
import com.suvikollc.resume_rag.dto.EmailDTO;
import com.suvikollc.resume_rag.service.EmailService;

import jakarta.annotation.PostConstruct;

@Service
public class EmailConsumer {

	private QueueClient emailQueueClient;

	@Autowired
	private EmailService emailService;

	Logger log = LoggerFactory.getLogger(EmailConsumer.class);

	private ObjectMapper objectMapper;

	@Value("${azure.storage.connection-string}")
	private String connectionString;

	@PostConstruct
	public void init() {

		this.emailQueueClient = new QueueClientBuilder().connectionString(connectionString)
				.queueName(CommunicationType.EMAIL.getQueueName()).buildClient();
		this.objectMapper = new ObjectMapper().findAndRegisterModules()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).registerModule(new JavaTimeModule());
	}

	@SuppressWarnings("deprecation")
	@Scheduled(fixedDelay = 50000)
	public void checkForNewEmails() {

		log.info("Checking for new emails in the queue...");

		for (QueueMessageItem message : emailQueueClient.receiveMessages(1)) {

			try {

				CommunicationDTO comDto = objectMapper.readValue(message.getMessageText(), CommunicationDTO.class);

				EmailDTO deserializedPayload = objectMapper.convertValue(comDto.getPayload(), EmailDTO.class);

				log.info("Processing message from queue: {}", message.getMessageText());
				emailService.sendEmail(deserializedPayload);

				emailQueueClient.deleteMessage(message.getMessageId(), message.getPopReceipt());
				log.info("Message processed and deleted from queue");
			} catch (Exception e) {
				log.error("Failed to parse message from queue: {}", message.getMessageText(), e);
				continue;
			}
		}

	}

}
