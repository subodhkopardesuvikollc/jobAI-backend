package com.suvikollc.resume_rag.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.suvikollc.resume_rag.serviceImpl.AcsMediaWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	@Autowired
	private AcsMediaWebSocketHandler acsMediaWebSocketHandler;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

		registry.addHandler(acsMediaWebSocketHandler, "/communication/call/acs/media").setAllowedOrigins("*");

	}

}
