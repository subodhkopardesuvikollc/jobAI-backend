package com.suvikollc.resume_rag.serviceImpl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.suvikollc.resume_rag.dto.ContactInfoDTO;
import com.suvikollc.resume_rag.service.ContactParserService;

@Service
public class ContactParserServiceImpl implements ContactParserService {

	private static final Pattern PHONE_PATTERN = Pattern
			.compile("(?:\\+?(\\d{1,3})[-\\s.]?)?\\(?(\\d{3})\\)?[-\\s.]?(\\d{3})[-\\s.]?(\\d{4})");

	private static final Pattern EMAIL_PATTERN = Pattern
			.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

	@Override
	public ContactInfoDTO extractContactInfo(String text) {
		String email = extractEmail(text);
		String phoneNo = extractPhoneNo(text);
		return new ContactInfoDTO(email, phoneNo);
	}

	@Override
	public String extractEmail(String text) {
		Matcher matcher = EMAIL_PATTERN.matcher(text);
		return matcher.find() ? matcher.group() : null;
	}

	@Override
	public String extractPhoneNo(String text) {
		Matcher matcher = PHONE_PATTERN.matcher(text);
		if (matcher.find()) {
			String countryCode = matcher.group(1);
			if (countryCode == null || countryCode.isEmpty()) {
				countryCode = "1";
			}
			String areaCode = matcher.group(2);
			String centralOffice = matcher.group(3);
			String lineNumber = matcher.group(4);
			return String.format("+%s%s%s%s", countryCode, areaCode, centralOffice, lineNumber);
		}

		return matcher.find() ? matcher.group() : null;
	}

}
