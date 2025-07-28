package com.suvikollc.resume_rag.serviceImpl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.suvikollc.resume_rag.dto.EmailDTO;
import com.suvikollc.resume_rag.service.EmailService;
import com.suvikollc.resume_rag.service.JDService;
import com.suvikollc.resume_rag.service.ResumeService;

@Service
public class EmailServiceImpl implements EmailService {

	Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

	@Autowired
	ChatClient chatClient;

	@Autowired
	JDService jdService;

	@Autowired
	ResumeService resumeService;

	@Override
	public EmailDTO generateCustomReachOutEmail(String resumeBlobName, String jobTitle, String jdBlobName) {

		String jdKeywords = jdService.generateKeywords(jdBlobName);

		List<Document> retrievedDocs = resumeService.retrieveRelavantCandidateWork(resumeBlobName, jobTitle,
				jdKeywords);

		String candidateWorkSnippets;
		String generatedEmailSubject;
		String generatedEmailBody;

		if (!retrievedDocs.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < Math.min(retrievedDocs.size(), 2); i++) {
				Document doc = retrievedDocs.get(i);
				sb.append("--- Section: ").append(doc.getMetadata().get("section")).append(" ---\n");
				sb.append(doc.getFormattedContent().trim()).append("\n");
				if (i < Math.min(retrievedDocs.size(), 2) - 1) {
					sb.append("\n");
				}
			}
			candidateWorkSnippets = sb.toString().trim();
			System.out.println("Retrieved snippets for LLM:\n" + candidateWorkSnippets);
		} else {
			System.out.println(
					"No specific experience/project snippets found for " + resumeBlobName + ". Using fallback.");
			candidateWorkSnippets = "their impressive background and achievements";
		}

		List<Message> messages = new ArrayList<>();

		messages.add(new SystemMessage(
				"You are an enthusiastic and professional talent acquisition specialist. Your goal is to write a concise, engaging, and highly personalized initial outreach email to a passive candidate. The email must highlight *one or two specific impressive achievements or projects* from the provided candidate work examples to show genuine interest. The tone should be warm, respectful, and encouraging. Avoid generic phrases and the full job description. Encourage a reply for a brief introductory call. Provide only the email subject and body, clearly separated."));

		messages.add(new UserMessage("Generate an outreach email for a candidate." + "\nRole: " + jobTitle
				+ "\nCandidate's Key Experience/Project Snippets:\n" + candidateWorkSnippets
				+ "\n\nOur Company's Value Proposition (Optional, keep it short): We're a fast-growing tech company building innovative AI solutions for sustainable energy, offering a collaborative environment and cutting-edge projects."
				+ "\n\nFormat as:" + "\nSubject: <Generated Subject Line>" + "\n\nBody: <Generated HTML Email Body>"));

		String rawGeneratedText = chatClient.prompt(new Prompt(messages)).call().content();

		log.info("Generated Email Content: \n" + rawGeneratedText);

		int subjectIndex = rawGeneratedText.indexOf("Subject:");
		int bodyIndex = rawGeneratedText.indexOf("\n\nBody:");

		if (subjectIndex != -1 && bodyIndex != -1 && bodyIndex > subjectIndex) {
			generatedEmailSubject = rawGeneratedText.substring(subjectIndex + "Subject: ".length(), bodyIndex).trim();
			generatedEmailBody = rawGeneratedText.substring(bodyIndex + "\n\nBody: ".length()).trim();
		} else {
			System.err.println(
					"Failed to parse subject and body from OpenAI response. Using fallback subject and full response as body.");
			generatedEmailSubject = "Exciting Opportunity: " + jobTitle;
			generatedEmailBody = rawGeneratedText;
		}
		if (!generatedEmailBody.startsWith("<html>")) {
			generatedEmailBody = "<html><body>" + generatedEmailBody.replace("\n", "<br/>") + "</body></html>";
		}

		return new EmailDTO(generatedEmailSubject, generatedEmailBody);
	}

}
