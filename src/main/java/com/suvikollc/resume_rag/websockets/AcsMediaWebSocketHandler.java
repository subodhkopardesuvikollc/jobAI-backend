package com.suvikollc.resume_rag.websockets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class AcsMediaWebSocketHandler extends TextWebSocketHandler {
	private WebSocketSession acsSession;
	
	@Autowired
	private ACSSessionManager acsSessionManager;

	@Autowired
	private AzureVoiceLiveWebSocketClient webSocketClient;

	private ObjectMapper objectMapper = new ObjectMapper();
	Logger log = LoggerFactory.getLogger(AcsMediaWebSocketHandler.class);

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		acsSessionManager.setSession(session);
		webSocketClient.connect().thenRun(() -> {
			log.info("Connected to Azure OpenAI WebSocket");
		}).exceptionally(ex -> {
			log.error("Failed to connect to Azure OpenAI WebSocket: {}", ex.getMessage());
			return null;
		});
		log.info("WebSocket connection established with ACS Media Service");
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {

		try {
			String payload = message.getPayload();
			JsonNode json = objectMapper.readTree(payload);
			if ("AudioData".equals(json.get("kind").asText()) && !json.get("audioData").get("silent").asBoolean()) {
				ObjectNode audioNode = objectMapper.createObjectNode();
				audioNode.put("type", "input_audio_buffer.append");
				audioNode.put("audio", json.get("audioData").get("data").asText());
				audioNode.put("event_id", "");
				webSocketClient.sendMessage(new TextMessage(objectMapper.writeValueAsString(audioNode)));

			}
		} catch (Exception e) {
			log.error("Error processing text message: {}", e.getMessage());
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		log.info("WebSocket connection closed with ACS Media Service: {}", status);
		this.acsSession = null;
	}

}
