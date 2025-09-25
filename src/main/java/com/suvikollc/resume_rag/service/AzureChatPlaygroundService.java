package com.suvikollc.resume_rag.service;

import com.suvikollc.resume_rag.dto.ChatResponse;

public interface AzureChatPlaygroundService {
	
	ChatResponse getChatResponse(String jdSummary, String conversationHistory);

}
