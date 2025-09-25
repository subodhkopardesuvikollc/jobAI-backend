package com.suvikollc.resume_rag.service;

import com.suvikollc.resume_rag.dto.WhatsAppSendResponse;

public interface WhatsAppService {
	
	Object sendMessage(String resumeId, String jdId);
	
	WhatsAppSendResponse replyToMessage(String waId, String message);

}
