package com.suvikollc.resume_rag.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {

	Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	VectorDBServiceImpl vectorDBService;

	@KafkaListener(topics = "resume-upload-events", groupId = "resume-group")
	public void listenForNewFile(String fileName) {

		log.info("Recieved event for fileName: " + fileName);

		try {

			vectorDBService.uploadToVectorDB(fileName);

			log.info("Successfully processed event for fileName: " + fileName);

		} catch (Exception e) {

			log.error("Failed to process event for fileName: " + fileName + " - " + e.getMessage());
		}

	}

}
