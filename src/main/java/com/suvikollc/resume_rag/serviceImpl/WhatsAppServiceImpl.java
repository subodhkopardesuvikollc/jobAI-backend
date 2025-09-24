package com.suvikollc.resume_rag.serviceImpl;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.suvikollc.resume_rag.dto.CommunicationDTO.CommunicationType;
import com.suvikollc.resume_rag.dto.Utterance;
import com.suvikollc.resume_rag.dto.WhatsAppContentDTO;
import com.suvikollc.resume_rag.dto.WhatsAppContentDTO.status;
import com.suvikollc.resume_rag.entities.Communication;
import com.suvikollc.resume_rag.entities.Jd;
import com.suvikollc.resume_rag.entities.Resume;
import com.suvikollc.resume_rag.repository.CommunicationRepository;
import com.suvikollc.resume_rag.repository.JdRepository;
import com.suvikollc.resume_rag.repository.ResumeRepository;
import com.suvikollc.resume_rag.service.ResumeService;
import com.suvikollc.resume_rag.service.WhatsAppService;

import jakarta.annotation.PostConstruct;

@Service
public class WhatsAppServiceImpl implements WhatsAppService {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private RestClient restClient;

	@Autowired
	private ResumeService resumeService;

	@Autowired
	private ResumeRepository resumeRepository;

	@Autowired
	private JdRepository jdRepository;

	@Value("${whatsapp.api.url}")
	private String whatsappApiUrl;

	@Value("${whatsapp.api.token}")
	private String whatsappApiToken;

	@Value("${whatsapp.api.phone-number-id}")
	private String whatsappPhoneNumberId;

	@Autowired
	private CommunicationRepository communicationRepository;

	@PostConstruct
	public void init() {

		restClient = RestClient.builder().baseUrl(whatsappApiUrl + "/" + whatsappPhoneNumberId)
				.defaultHeader("Authorization", "Bearer " + whatsappApiToken).build();
	}

	@Override
	public Object sendMessage(String resumeId, String jdId) {

		var communication = communicationRepository.findByJdIdAndResumeIdAndType(new ObjectId(jdId),
				new ObjectId(resumeId), CommunicationType.WHATSAPP);
		if (communication == null) {
			log.info("No previous whatsapp communication found, sending initial message");
			communication = new Communication();
			communication.setJdId(new ObjectId(jdId));
			communication.setResumeId(new ObjectId(resumeId));
			communication.setType(CommunicationType.WHATSAPP);
			communication.setContent(new WhatsAppContentDTO());
			return sendInitilaTemplateMessage(communication);
		}
		log.info("Previous whatsapp communication found, not sending initial message");
		return null;

	}

	private Object sendInitilaTemplateMessage(Communication communication) {
		String InitialMessage = "Hello %s Are you looking for a new role? We have a job requirement that we are excited to discuss with you. Job Title:%s";

		Resume resume = resumeRepository.findById(communication.getResumeId())
				.orElseThrow(() -> new RuntimeException("Resume not found with id: " + communication.getResumeId()));
		Jd jd = jdRepository.findById(communication.getJdId())
				.orElseThrow(() -> new RuntimeException("Jd not found with id: " + communication.getJdId()));
		String jdBlobName = jd.getBlobName();
		String resumeBlobName = resume.getBlobName();
		String phoneNumber = resumeService.getPhoneNo(resume.getBlobName());

		ObjectNode payload = new ObjectMapper().createObjectNode();

		payload.put("messaging_product", "whatsapp");
		payload.put("to", phoneNumber);
		payload.put("type", "template");
		ObjectNode template = payload.putObject("template");
		template.put("name", "hello_world");
		ObjectNode language = template.putObject("language");
		language.put("code", "en_US");
//		ArrayNode components = new ObjectMapper().createArrayNode();
//
//		ObjectNode headerComponent = new ObjectMapper().createObjectNode();
//		headerComponent.put("type", "header");
//		ArrayNode headerParameters = new ObjectMapper().createArrayNode();
//		headerParameters.add(
//				new ObjectMapper().createObjectNode().put("type", "text").put("text", formatJdFileName(jdBlobName)));
//		headerComponent.set("parameters", headerParameters);
//		components.add(headerComponent);
//
//		ObjectNode bodyComponent = new ObjectMapper().createObjectNode();
//		bodyComponent.put("type", "body");
//		ArrayNode bodyParameters = new ObjectMapper().createArrayNode();
//		bodyParameters.add(new ObjectMapper().createObjectNode().put("type", "text").put("text",
//				formatJdFileName(resumeBlobName)));
//		bodyParameters.add(
//				new ObjectMapper().createObjectNode().put("type", "text").put("text", formatJdFileName(jdBlobName)));
//		bodyComponent.set("parameters", bodyParameters);
//		components.add(bodyComponent);
//
//		template.set("components", components);

		payload.set("template", template);

		try {

			var response = restClient.post().uri("/messages").body(payload).retrieve()
					.body(new ParameterizedTypeReference<Object>() {
					});

			log.info("WhatsApp message sent successfully to {}: {}", phoneNumber, response);

			WhatsAppContentDTO content = new WhatsAppContentDTO();
			content.setChatStatus(status.CHAT_INITIATED);
			Utterance utterance = new Utterance();
			utterance.setSpeaker("model");
			utterance
					.setText(String.format(InitialMessage, formatFileName(resumeBlobName), formatFileName(jdBlobName)));
			content.getUtterances().add(utterance);
			communication.setContent(content);

			communicationRepository.save(communication);
			return response;

		} catch (Exception e) {
			log.error("Failed to send WhatsApp message to {}: {}", phoneNumber, e.getMessage());
			throw new RuntimeException("Failed to send WhatsApp message to " + phoneNumber, e);
		}
	}

	private String formatFileName(String jdBlobName) {
		if (jdBlobName == null || jdBlobName.isEmpty()) {
			return "the job description";
		}
		String fileName = jdBlobName.contains(".") ? jdBlobName.substring(0, jdBlobName.lastIndexOf('.')) : jdBlobName;
		fileName = fileName.replaceAll("[-_]", " ");
		String[] words = fileName.split("\\s+");
		if (words.length >= 2) {
			return words[0] + " " + words[1];
		} else if (words.length == 1) {
			return words[0];
		}
		return fileName;
	}
}
