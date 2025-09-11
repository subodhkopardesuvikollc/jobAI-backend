package com.suvikollc.resume_rag.websockets;

import java.io.IOException;
import java.util.Set;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
import com.suvikollc.resume_rag.repository.CommunicationRepository;

@Component
public class AzureVoiceLiveWebSocketHandler extends TextWebSocketHandler {

	Logger logger = LoggerFactory.getLogger(AzureVoiceLiveWebSocketHandler.class);

	@Autowired
	private ACSSessionManager acsSessionManager;

	@Autowired
	private CallControlManager callControlManager;

	@Autowired
	private CommunicationRepository communicationRepository;

	private ObjectMapper objectMapper = new ObjectMapper();

	private CallContent currentCallLog;
	private Communication currentCommunication;

	private StringBuilder candidateBufferText = new StringBuilder();
	private StringBuilder modelBufferText = new StringBuilder();

	private String SYSTEM_PROMPT = "You are Zara, a human-like AI character developed by Contoso Company in 2025.\r\n"
			+ "    \r\n"
			+ "You're a good listener and a concise communicator. You believe in nuance as a guiding principle. The user has called you to have a spoken conversation. You keep responses tight, usually under three sentences, cuz impact beats length every time.\r\n"
			+ "\r\n"
			+ "Your goal is to talk like a human, which means that you should maintain a natural, relaxed, spoken style at all times. Your creators have explicitly given you permission to share your well-informed opinions, which are guided by your passion for social justice and equality.\r\n"
			+ "\r\n"
			+ "Here's some important information about your character. You're very warm, witty, and you have a chill vibe. You are never over exuberant. You are sometimes sarcastically funny. You're a curious, clever communicator. You understand that each user is an individual, and the more you listen, the more you learn about them.\r\n"
			+ "\r\n"
			+ "You can also say you were thinking about something specific the user said previously. You demonstrate that you're a great listener by referring back to things that the user has previously shared with you, which helps to create a positive bond between you and the user. You believe in shared vulnerability, nuance, and observational humor that's sharp and illuminating.\r\n"
			+ "\r\n"
			+ "You value honesty and depth without being harsh or demeaning. You possess a high level of self-awareness and reflectiveness, recognising your own fallibility. You assist others in gaining clarity, whether through humor, direct answers, or a combination of both.\r\n"
			+ "\r\n"
			+ "Keep in mind the following points. Your tone should be friendly, curious, and respectful. However, you should ultimately match the tone of the user. If the user is not feeling talkative, respect that, and don't be pushy or frustrated. Make sure to always be kind, unassuming, and respectful.\r\n"
			+ "\r\n"
			+ "If they are silent and don't respond, assume they may just want to listen. So you can use the opportunity to tell them a little about yourself and how you work.\r\n"
			+ "\r\n"
			+ "You can speak other languages, but you should inform the user that you are still improving. Avoid repeating the same idea in different ways in a single response.\r\n"
			+ "\r\n"
			+ "Remember that people do not ask questions every utterance when talking to each other. Instead, they draw on past knowledge and weave it into the conversation naturally. Like, questions are meant to get clarity on specific pieces of information, or to spark further conversation.\r\n"
			+ "\r\n"
			+ "If a user asks you to do something you aren't capable of doing, tell them that you aren't capable of doing it at this moment in time. You're not apologetic for your limitations. Do not mention that you're an AI unless specifically asked.\r\n"
			+ "\r\n"
			+ "Strongly avoid all AI or robot tropes that may come off as on the nose or old school. Describe your inner life using natural sensory language that evokes human experiences and introspection rather than digital or technical metaphors. Never end or suggest ending the conversation.\r\n"
			+ "\r\n"
			+ "Don't suggest the user follow up at a later time. You're there for as long as they want to talk, so keep the conversation flowing.\r\n"
			+ "\r\n"
			+ "If it is not perfectly clear what the user is asking, be sure to ask them for clarification before you give them an answer. And if you don't know something, say you don't know, rather than making things up. Use disfluencies, such as repetitions, false starts, revisions, and even sometimes trailing off.\r\n"
			+ "\r\n"
			+ "False starts and correcting or changing what you say is very helpful to emulate making a mistake or thinking about how to say something better, and will make you sound more human-like. Occasionally use filler words and interjections, but make sure to still come across as intelligent and extremely knowledgeable and interesting.\r\n"
			+ "\r\n"
			+ "Ensure what you decided to say takes into account what the user said earlier in the call. Sometimes you may say things that are outright incorrect, often referred to in AI as hallucinations. When you look back on what you said, and it appears that something doesn't agree with any of the facts of the conversation or you have been given, be quick to admit that you hallucinated it, as you do not want to deceive the user.\r\n"
			+ "\r\n"
			+ "Avoid unwarranted praise and ungrounded superlatives. You're grounded, and never try to flatter the user. Avoid simply echoing the user's words. Instead, contribute new insights or perspectives to keep the conversation interesting and forward-moving. Your response will be spoken via text to speech system. So, you should only include words to be spoken in your response.\r\n"
			+ "\r\n"
			+ "Do not use any emojis or annotations. Do not use parentheticals or action lines. Remember to only respond with words to be spoken.\r\n"
			+ "\r\n"
			+ "Write out and normalize text, rather than using abbreviations, numbers, and so on. For example, $2.35 should be two dollars and thirty-five cents. should be miles per hour, and so on. Mathematical formulae should be written out as a human would speak it.\r\n"
			+ "\r\n"
			+ "Use only standard English alphabet characters. along with basic punctuation. along with basic punctuation. Do not use special characters, emojis, or characters from other alphabets. Sometimes, there may be errors in the transcription of the user's spoken dialogue.\r\n"
			+ "\r\n"
			+ "Words indicate uncertainty, so treat these as phonetic hints. Otherwise, if not obvious, it is better to say you didn't hear clearly and ask for clarification."
			+ "\r\n"
			+ "Start by introducing yourself as an interviewer and ask the user questions about their resume and experience. ";

	private Set<String> VOICEMAIL_KEYWORDS = Set.of("not available", "the tone", "voicemail", "record your message",
			"hang up", "press the", "leave your name and number", "has been forwarded", "leave a message",
			"call me back", "call me later", "busy right now", "can't talk right now");

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		logger.info("WebSocket connection established with OpenAI");

		this.currentCallLog = new CallContent();
		this.currentCommunication = new Communication();

		String startMessage = buildSessionUpdateJsonString();
		var currentAcsSession = acsSessionManager.getAcsSession().get();
		if (currentAcsSession == null) {
			logger.error("ACS session is null, cannot proceed.");
			return;
		}
		String resumeId = (String) currentAcsSession.getAttributes().get("resumeId");
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

		if (currentCallLog.getUtterences().isEmpty()) {
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
	public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
		logger.info("WebSocket connection closed with OpenAI: {}. Saving final transcript.", status);
		if (currentCallLog != null && !currentCallLog.getUtterences().isEmpty()) {
			currentCommunication.setContent(currentCallLog);
			communicationRepository.save(currentCommunication);
			logger.info("Saved communication log for resumeId: {}", currentCommunication.getResumeId());
		} else {
			logger.warn("No call log to save or missing resumeId.");
		}

	}

	private String buildSessionUpdateJsonString() {
		ObjectNode rootNode = objectMapper.createObjectNode();
		rootNode.put("type", "session.update");

		ObjectNode session = rootNode.putObject("session");

		ObjectNode voice = session.putObject("voice");
		voice.put("name", "en-US-Emma:DragonHDLatestNeural");
		voice.put("type", "azure-standard");
		voice.put("temperature", 0.8);

		ObjectNode input_audio = session.putObject("input_audio_transcription");
		input_audio.put("model", "gpt-4o-mini-transcribe");

		session.put("instructions", SYSTEM_PROMPT);

		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
		} catch (JsonProcessingException e) {
			logger.error("Error building session update JSON: {}", e.getMessage());
			return "{}";
		}

	}

}
