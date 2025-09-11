package com.suvikollc.resume_rag.interceptor;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ACSWebSocketInterceptor implements HandshakeInterceptor {

	Logger log = LoggerFactory.getLogger(ACSWebSocketInterceptor.class);

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) throws Exception {

		try {
			String resumeId = UriComponentsBuilder.fromUriString(request.getURI().toString()).build().getQueryParams()
					.getFirst("resumeId");
			if (resumeId != null && !resumeId.isEmpty()) {
				attributes.put("resumeId", resumeId);
			} else {
				log.error("Resume ID is missing in the WebSocket handshake request");

			}
		} catch (Exception e) {
			log.error("Error in ACSWebSocketInterceptor beforeHandshake: {}", e.getMessage());
		}
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			@Nullable Exception exception) {
		// TODO Auto-generated method stub

	}

}
