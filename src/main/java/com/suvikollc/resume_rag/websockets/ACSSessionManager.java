package com.suvikollc.resume_rag.websockets;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import lombok.Getter;

@Service
@Getter
public class ACSSessionManager {

	private final AtomicReference<WebSocketSession> acsSession = new AtomicReference<>();

	public void setSession(WebSocketSession session) {
		acsSession.set(session);
	}

	public void clearSession() {
		acsSession.set(null);
	}

}
