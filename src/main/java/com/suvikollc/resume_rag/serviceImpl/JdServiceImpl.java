package com.suvikollc.resume_rag.serviceImpl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.suvikollc.resume_rag.entities.Jd;
import com.suvikollc.resume_rag.repository.JdRepository;
import com.suvikollc.resume_rag.service.FileService;
import com.suvikollc.resume_rag.service.JDService;

@Service
public class JdServiceImpl implements JDService {

	Logger log = LoggerFactory.getLogger(JdServiceImpl.class);

	@Autowired
	FileService fileService;

	@Value("${azure.storage.jd.container.name}")
	private String jdContainerName;

	@Autowired
	private ChatClient chatClient;

	@Autowired
	private JdRepository jdRepository;

	@Override
	public String generateKeywords(String jdBlobName) {

		var blobClient = fileService.getBlobClient(jdBlobName, jdContainerName);
		if (!blobClient.exists()) {
			throw new RuntimeException("Blob does not exist for blob name: " + jdBlobName);
		}

		Jd jd = jdRepository.findByFileName(jdBlobName);

		if (jd == null) {
			throw new RuntimeException("JD not found in the database for blob name: " + jdBlobName);
		}
		if (jd != null && jd.getKeywords() != null) {
			log.info("Keywords already exist for JD: {}", jdBlobName);
			return jd.getKeywords();
		}
		try (InputStream inputStream = blobClient.openInputStream()) {
			String jdContent = fileService.extractContent(inputStream);

			List<Message> messages = new ArrayList<>();
			messages.add(new SystemMessage(
					"You are an expert recruitment analyst. Your task is to extract the most critical keywords, skills, technologies, and responsibilities from a job description. Focus on terms that would be essential for matching a candidate to this role. Provide them as a comma-separated list. Do NOT include any other text or formatting."));
			messages.add(new UserMessage("Extract keywords from the following job description: " + jdContent));

			Prompt prompt = new Prompt(messages);

			String keywords = chatClient.prompt(prompt).call().content();

			System.out.println("Extracted Keywords: " + keywords);
			jd.setKeywords(keywords);

			jdRepository.save(jd);

			return keywords;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to extract keywords from JD: " + e.getMessage());
		}

	}

	public String generateSummary(String jdBlobName) {

		var blobClient = fileService.getBlobClient(jdBlobName, jdContainerName);
		if (!blobClient.exists()) {
			throw new RuntimeException("Blob does not exist for blob name: " + jdBlobName);
		}

		Jd jd = jdRepository.findByFileName(jdBlobName);

		if (jd == null) {
			throw new RuntimeException("JD not found in the database for blob name: " + jdBlobName);
		}
		if (jd != null && jd.getSummary() != null && !jd.getSummary().isEmpty()) {
			log.info("Summary already exists for JD: {}", jdBlobName);
			return jd.getSummary();
		}

		try (InputStream inputStream = blobClient.openInputStream()) {
			String jdContent = fileService.extractContent(inputStream);

			String systemMessageText = "You are an expert recruitment analyst. Your task is to write a brief, natural language summary of the provided job description. The summary should be a single, easy-to-read paragraph that highlights the most critical information: the job title, required years of experience, key responsibilities, and mandatory skills. The tone should be professional yet engaging, as if you were briefly describing the role to a potential candidate. Do not use bullet points, lists, or any special formatting. Respond ONLY with the summary paragraph.";
			String userMessageText = "Create a summary for the following job description: " + jdContent;

			List<Message> messages = new ArrayList<>();
			messages.add(new SystemMessage(systemMessageText));
			messages.add(new UserMessage(userMessageText));

			Prompt prompt = new Prompt(messages);

			String summary = chatClient.prompt(prompt).call().content();

			System.out.println("Generated Summary: " + summary);

			jd.setSummary(summary);
			jdRepository.save(jd);

			return summary;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to generate summary for JD: " + e.getMessage());
		}
	}

	@Override
	public String generateSummary(ObjectId jdId) {
		Jd jd = jdRepository.findById(jdId).orElse(null);
		if (jd == null) {
			throw new RuntimeException("JD not found in the database for ID: " + jdId);
		}
		return generateSummary(jd.getBlobName());
	}

}
