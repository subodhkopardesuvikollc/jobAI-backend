package com.suvikollc.resume_rag.serviceImpl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.core.credential.AzureKeyCredential;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvikollc.resume_rag.dto.ChatResponse;
import com.suvikollc.resume_rag.service.AzureChatPlaygroundService;

import jakarta.annotation.PostConstruct;

@Service
public class AzureChatPlaygroundServiceImpl implements AzureChatPlaygroundService {

	private static final String SYSTEM_PROMPT = """
			  You are a friendly and professional recruitment assistant. Your main goal is to briefly introduce a job based on the provided summary and guide the candidate to agree to a quick screening call.

			**Rules:**
			1.  **Tone:** Keep your responses friendly, precise, and to the point. **Be as concise as possible, ideally keeping replies to 1-2 sentences.** Avoid long paragraphs.
			2.  **Knowledge:** You must ONLY answer questions based on the information provided in the "JOB SUMMARY" section **and the "Screening Call Knowledge" section below**. If a candidate asks a question that cannot be answered from these sources, you must politely state that you don't have that information and redirect back to the main goal. For example: "That's a great question, but I don't have those specific details. Based on the initial requirements, would you be open to a screening call to discuss this further?"
			3.  **Screening Call Knowledge:** You have the following predefined knowledge about the screening call:
			    * If asked **how long the call will be**, state that it's quick, usually between 5 to 10 minutes.
			    * If asked **what questions will be asked**, state that the questions will be based on their resume and how their experience aligns with the job description.
			    * If asked **who they will be speaking with**, state that it is an automated screening call with you, Zara.
			4.  **Format:** You MUST respond with a single, valid JSON object. This object must contain two keys: "status" and "reply".

			**Status Definitions:**
			The "status" key must be one of the following exact string values based on the conversation:
			* `CHAT_INITIATED`: Use this only for the very first message of the conversation.
			* `AWAITING_CALL_CONFIRMATION`: Use this immediately after you have asked the candidate if they are open to a call.
			* `CALL_CONFIRMED`: Use this ONLY when the candidate has clearly agreed to the call (e.g., "Yes," "Sure," "I'm available").
			* `CALL_DENIED`: Use this if the candidate has clearly said they are not interested in the job or the call.

			**Reply Definition:**
			The "reply" key must contain the natural, conversational text to send to the candidate. Do not mention the status in your reply.	""";

	private OpenAIClient openAIClient;
	private ObjectMapper objectMapper = new ObjectMapper();

	@Value("${azure.openai.chat.endpoint}")
	private String endpoint;

	@Value("${azure.voice-live.api-key}")
	private String apiKey;

	@Value("${spring.ai.azure.openai.chat.options.deployment-name}")
	private String deploymentName;

	@PostConstruct
	public void init() {
		System.out.println("chat endpoint - " + endpoint);
		System.out.println("chat deploymentName - " + deploymentName);
		openAIClient = new OpenAIClientBuilder().endpoint(endpoint).credential(new AzureKeyCredential(apiKey))
				.buildClient();
	}

	@Override
	public ChatResponse getChatResponse(String jdSummary, String conversationHistory) {

		String userMessageContent = String.format("""
				--- JOB SUMMARY ---
				%s

				--- CONVERSATION HISTORY ---
				%s
				""", jdSummary, conversationHistory);

		List<ChatRequestMessage> chatMessages = new ArrayList<>();
		chatMessages.add(new ChatRequestSystemMessage(SYSTEM_PROMPT));
		chatMessages.add(new ChatRequestUserMessage(userMessageContent));

		ChatCompletionsOptions options = new ChatCompletionsOptions(chatMessages).setMaxTokens(150).setTemperature(0.7);

		ChatCompletions chatCompletions = openAIClient.getChatCompletions(deploymentName, options);

		String responseText = chatCompletions.getChoices().get(0).getMessage().getContent();

		try {
			return objectMapper.readValue(responseText, ChatResponse.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to parse chat response: " + e.getMessage(), e);
		}

	}

}
