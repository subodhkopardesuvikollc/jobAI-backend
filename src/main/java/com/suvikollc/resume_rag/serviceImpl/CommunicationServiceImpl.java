package com.suvikollc.resume_rag.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvikollc.resume_rag.dto.CommunicationDTO;
import com.suvikollc.resume_rag.service.CommunicationSerivce;

@Service
public class CommunicationServiceImpl implements CommunicationSerivce {

	@Value("${azure.storage.connection-string}")
	private String connectionString;

	private final ObjectMapper objectMapper = new ObjectMapper();

	Logger log = LoggerFactory.getLogger(CommunicationServiceImpl.class);

	@Override
	public String produceCommunication(CommunicationDTO comDto) {

		String queueName = comDto.getType().getQueueName();

		try {
			QueueClient queueClient = new QueueClientBuilder().connectionString(connectionString).queueName(queueName)
					.buildClient();

			queueClient.createIfNotExists();

			String message = objectMapper.writeValueAsString(comDto);

			queueClient.sendMessage(message);
			log.info("Message sent to queue: {}", queueName);

			return "Message sent to queue: " + queueName;
		} catch (Exception e) {
			throw new RuntimeException("Failed to send message to queue: " + queueName, e);
		}
	}

}
