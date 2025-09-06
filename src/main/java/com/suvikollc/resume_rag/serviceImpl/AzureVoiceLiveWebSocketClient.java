package com.suvikollc.resume_rag.serviceImpl;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Component
public class AzureVoiceLiveWebSocketClient {

	@Value("${azure.voice-live.websocket.url}")
	private String webSocketUrl;
	@Value("${azure.voice-live.api-key}")
	private String apiKey;

	private WebSocketSession session;
	@Autowired
	private AzureVoiceLiveWebSocketHandler webSocketHandler;

	public CompletableFuture<Void> connect() {
		CompletableFuture<Void> future = new CompletableFuture<>();

		StandardWebSocketClient client = new StandardWebSocketClient();
		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.add("api-key", apiKey);

		client.execute(webSocketHandler, headers, URI.create(webSocketUrl)).thenApply(session -> {
			this.session = session;
			future.complete(null);
			return null;
		}).exceptionally(ex -> {
			future.completeExceptionally(ex);
			return null;
		});
		return future;

	}

	public void sendMessage(TextMessage message) {
		if (session != null && session.isOpen()) {
			try {
				session.sendMessage(message);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void close() {
		try {
			if (session != null && session.isOpen()) {
				session.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
