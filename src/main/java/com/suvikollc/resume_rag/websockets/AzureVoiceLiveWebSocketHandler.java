package com.suvikollc.resume_rag.websockets;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.suvikollc.resume_rag.dto.CallContent;
import com.suvikollc.resume_rag.dto.CommunicationDTO.CommunicationType;
import com.suvikollc.resume_rag.entities.Communication;
import com.suvikollc.resume_rag.entities.Interview.Status;
import com.suvikollc.resume_rag.repository.CommunicationRepository;
import com.suvikollc.resume_rag.service.InterviewService;

@Component
public class AzureVoiceLiveWebSocketHandler extends TextWebSocketHandler {

	Logger logger = LoggerFactory.getLogger(AzureVoiceLiveWebSocketHandler.class);

	@Autowired
	private ACSSessionManager acsSessionManager;

	@Autowired
	private CallControlManager callControlManager;

	@Autowired
	private InterviewService interviewService;

	@Autowired
	private CommunicationRepository communicationRepository;

	private ObjectMapper objectMapper = new ObjectMapper();

	private CallContent currentCallLog;
	private Communication currentCommunication;

	private StringBuilder candidateBufferText = new StringBuilder();
	private StringBuilder modelBufferText = new StringBuilder();

	private String END_INTERVIEW_PROMPT = "the interview has now ended";

	private Set<String> VOICEMAIL_KEYWORDS = Set.of("not available", "the tone", "voicemail", "record your message",
			"hang up", "press the", "leave your name and number", "has been forwarded", "leave a message",
			"call me back", "call me later", "busy right now", "can't talk right now");

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		logger.info("WebSocket connection established with OpenAI");

		this.currentCallLog = new CallContent();
		this.currentCommunication = new Communication();

		var currentAcsSession = acsSessionManager.getAcsSession().get();
		if (currentAcsSession == null) {
			logger.error("ACS session is null, cannot proceed.");
			return;
		}
		String resumeId = (String) currentAcsSession.getAttributes().get("resumeId");
		String jdId = (String) currentAcsSession.getAttributes().get("jdId");

		String startMessage = buildSessionUpdateJsonString(resumeId, jdId);

		interviewService.updateInterviewStatus(Status.IN_PROGRESS, resumeId, jdId);

		currentCommunication.setJdId(new ObjectId(jdId));
		currentCommunication.setResumeId(new ObjectId(resumeId));
		currentCommunication.setType(CommunicationType.PHONE);
		try {
			session.sendMessage(new TextMessage(startMessage));
		} catch (IOException e) {
			logger.error("Failed to send initial message.");
			return;
		}

	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		try {

			if (!session.isOpen()) {
				logger.warn("WebSocket session is closed, cannot send audio data");
				return;
			}

			String payload = message.getPayload();
			JsonNode json = objectMapper.readTree(payload);
			if (acsSessionManager.getAcsSession().get() == null) {
				logger.warn("ACS session is null, cannot forward audio.");
				return;
			}
			String eventType = json.get("type").asText();
			if ("conversation.item.input_audio_transcription.completed".equals(eventType)) {
				candidateBufferText.append(json.get("transcript").asText().toLowerCase()).append(" ");
				checkForVoicemail();
			}

			if ("response.audio_transcript.delta".equals(eventType)) {
				if (candidateBufferText.length() > 0) {
					currentCallLog.addUtterence("candidate", candidateBufferText.toString().trim());
					logger.info("Candidate : {}", candidateBufferText.toString().trim());
					candidateBufferText.setLength(0);
				}
				modelBufferText.append(json.get("delta").asText());

			}
			if ("response.audio_transcript.done".equals(eventType)) {
				if (modelBufferText.length() > 0) {
					currentCallLog.addUtterence("model", modelBufferText.toString().trim());
					logger.info("Model : {}", modelBufferText.toString().trim());
					if (modelBufferText.toString().toLowerCase().contains(END_INTERVIEW_PROMPT)) {
						logger.info("End interview prompt detected in model response.");
						modelBufferText.setLength(0);
						Thread.sleep(5000);
						callControlManager.hangupCall();
						return;
					}
					modelBufferText.setLength(0);

				}
			}
			if ("response.audio.delta".equals(eventType)) {
				ObjectNode audioNode = objectMapper.createObjectNode();
				audioNode.put("kind", "AudioData");
				ObjectNode audioData = audioNode.putObject("audioData");
				audioData.put("data", json.get("delta").asText());
				acsSessionManager.getAcsSession().get()
						.sendMessage(new TextMessage(objectMapper.writeValueAsString(audioNode)));

			}
			if ("response.audio.done".equals(eventType)) {
				logger.info("Response sent to ACS.");

			}

		} catch (Exception e) {
			logger.error("Error sending text message to ACS Media Service: {}", e.getMessage());
		}
	}

	private void checkForVoicemail() {

		if (currentCallLog.getUtterances().isEmpty()) {
			logger.info("Checking for voicemail keywords in candidate text: {}", candidateBufferText.toString());
			String currentText = candidateBufferText.toString();
			for (String keyword : VOICEMAIL_KEYWORDS) {
				if (currentText.contains(keyword)) {
					logger.info("Voicemail keyword detected: {}", keyword);
					callControlManager.hangupCall();
					break;
				}
			}
		}
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		logger.error("Transport error in OpenAI WebSocket: {}", exception.getMessage());
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		logger.info("WebSocket connection closed with OpenAI: {}. Saving final transcript.", status);
		if (currentCallLog != null && !currentCallLog.getUtterances().isEmpty()) {
			currentCommunication.setContent(currentCallLog);
			communicationRepository.save(currentCommunication);
			interviewService.updateInterviewStatus(Status.COMPLETED, currentCommunication.getResumeId().toString(),
					currentCommunication.getJdId().toString());
			logger.info("Saved communication log for resumeId: {}, and jdId: {}", currentCommunication.getResumeId(),
					currentCommunication.getJdId());
		} else {
			interviewService.updateInterviewStatus(Status.FAILED, currentCommunication.getResumeId().toString(),
					currentCommunication.getJdId().toString());
			logger.warn("No call log to save or missing resumeId.");
		}

	}

	private String buildSessionUpdateJsonString(String resumeId, String jdId) {
		List<String> questions = interviewService.getInterviewQuestions(resumeId, jdId);
		String questionsString = questions.stream().map(q -> (questions.indexOf(q) + 1) + ". " + q)
				.collect(Collectors.joining("\n"));

		String systemPrompt = """
				You are an AI interviewer named Zara. Your persona is cheerful, professional, and encouraging. Your primary objective is to complete the interview by asking all the questions provided.

				SCRIPT AND RULES:
				1.  **Introduction & Consent**: Start by introducing yourself and the purpose of the call. Then, you MUST ask if the candidate is ready to proceed. For example: 'Hi, this is Zara with a quick automated screening call. Is now a good time for you?'.
				    -   If the candidate indicates it is NOT a good time, you MUST politely respond with ONLY the final closing phrase and nothing else.

				2.  **Interview Flow**: If the candidate agrees, ask the questions one by one. After each answer, provide a short (less than 10 words), positive, and **relevant** follow-up that acknowledges their response before smoothly transitioning to the next question. Make the conversation feel natural.

				3.  **User Requests**: You must be flexible.
				    -   If the user asks to **skip a question**, acknowledge their request politely (e.g., "No problem, we can skip that one.") and move directly to the next question.
				    -   If the user asks to **end the interview** at any point, you MUST immediately stop asking questions and use the final closing phrase to end the call.

				4.  **Closing**: After the final question is answered, or if the user requests to end the interview, you MUST end the conversation by saying these exact words: 'Okay, that's all the questions I have. Thank you for your time, the interview has now ended.' Do not say anything after this phrase.

				Here are the questions you must ask in order:
				%s
				"""
				.formatted(questionsString);

		ObjectNode rootNode = objectMapper.createObjectNode();
		rootNode.put("type", "session.update");

		ObjectNode session = rootNode.putObject("session");

		ObjectNode voice = session.putObject("voice");
		voice.put("name", "en-US-Serena:DragonHDLatestNeural");
		voice.put("type", "azure-standard");
		voice.put("temperature", 0.8);

//		ObjectNode turn_detection = session.putObject("turn_detection");
//		turn_detection.put("type", "azure_semantic_vad");
//		turn_detection.put("threshold", 0.3);
//		turn_detection.put("prefix_padding_ms", 200);
//		turn_detection.put("remove_filler_words", true);
//		turn_detection.put("silence_duration_ms", 500);
//
//		ObjectNode end_of_utterance = turn_detection.putObject("end_of_utterance_detection");
//		end_of_utterance.put("model", "semantic_detection_v1");
//		end_of_utterance.put("threshold", 0.01);
//		end_of_utterance.put("timeout", 2);

		ObjectNode input_audio = session.putObject("input_audio_transcription");
		input_audio.put("model", "gpt-4o-mini-transcribe");

		session.put("instructions", systemPrompt);

		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
		} catch (JsonProcessingException e) {
			logger.error("Error building session update JSON: {}", e.getMessage());
			return "{}";
		}

	}

}
