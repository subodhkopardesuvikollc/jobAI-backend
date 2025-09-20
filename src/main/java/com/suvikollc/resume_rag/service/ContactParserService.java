package com.suvikollc.resume_rag.service;

import com.suvikollc.resume_rag.dto.ContactInfoDTO;

public interface ContactParserService {

	ContactInfoDTO extractContactInfo(String text);
	
	String extractEmail(String text);
	
	String extractPhoneNo(String text);
}
