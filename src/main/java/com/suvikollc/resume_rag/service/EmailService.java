package com.suvikollc.resume_rag.service;

import com.suvikollc.resume_rag.dto.EmailDTO;

public interface EmailService {
	
	EmailDTO generateCustomReachOutEmail(String resumeBlobName, String jobTitle, String jdBlobName);

}
