package com.suvikollc.resume_rag.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducer {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private static final String TOPIC = "resume-upload-events";

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	public void sendFileEvent(String fileName) {

		kafkaTemplate.send(TOPIC, fileName);

		log.info("Message sent for file name: " + fileName);

	}

}
