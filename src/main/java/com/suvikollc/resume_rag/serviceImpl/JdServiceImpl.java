package com.suvikollc.resume_rag.serviceImpl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.suvikollc.resume_rag.service.FileService;
import com.suvikollc.resume_rag.service.JDService;

@Service
public class JdServiceImpl implements JDService {

	@Autowired
	FileService fileService;

	@Value("${azure.storage.jd.container.name}")
	private String jdContainerName;

	@Autowired
	private ChatClient chatClient;

	@Override
	public String generateKeywords(String jdBlobName) {

		var blobClient = fileService.getBlobClient(jdBlobName, jdContainerName);

		try (InputStream inputStream = blobClient.openInputStream()) {
			String jdContent = fileService.extractContent(inputStream);

			List<Message> messages = new ArrayList<>();
			messages.add(new SystemMessage(
					"You are an expert recruitment analyst. Your task is to extract the most critical keywords, skills, technologies, and responsibilities from a job description. Focus on terms that would be essential for matching a candidate to this role. Provide them as a comma-separated list. Do NOT include any other text or formatting."));
			messages.add(new UserMessage("Extract keywords from the following job description: " + jdContent));

			Prompt prompt = new Prompt(messages);

			String keywords = chatClient.prompt(prompt).call().content();

			System.out.println("Extracted Keywords: " + keywords);

			return keywords;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to extract keywords from JD: " + e.getMessage());
		}

	}

}
