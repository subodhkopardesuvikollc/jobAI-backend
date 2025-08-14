package com.suvikollc.resume_rag.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.QueueMessageItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvikollc.resume_rag.dto.CommunicationDTO;
import com.suvikollc.resume_rag.dto.EmailDTO;
import com.suvikollc.resume_rag.service.EmailService;

@Service
public class EmailConsumer {
	
	private QueueClient emailQueueClient;
	
	@Autowired
	private EmailService emailService;
	
		
	Logger log = LoggerFactory.getLogger(EmailConsumer.class);
	
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	public EmailConsumer(@Value("${azure.storage.connection-string}") String connectionString) {
		
		this.emailQueueClient = new QueueClientBuilder()
				.connectionString(connectionString)
				.queueName("email-queue")
				.buildClient();
	}
	
	@SuppressWarnings("deprecation")
	@Scheduled(fixedDelay = 1000)
	public void checkForNewEmails() {
		
		log.info("Checking for new emails in the queue...");
		
		for(QueueMessageItem message: emailQueueClient.receiveMessages(1)) {
			
			try {
				
				CommunicationDTO comDto = objectMapper.readValue(message.getMessageText(), CommunicationDTO.class);
				
				EmailDTO deserializedPayload = objectMapper.convertValue(comDto.getPayload(), EmailDTO.class);

				log.info("Processing message from queue: {}", message.getMessageText());
				emailService.sendEmail(deserializedPayload);
				
				emailQueueClient.deleteMessage(message.getMessageId(), message.getPopReceipt());
				log.info("Message processed and deleted from queue");
			}
			catch (Exception e) {
				log.error("Failed to parse message from queue: {}", message.getMessageText(), e);
				continue;
			}
		}
		
	}
	
	

}
