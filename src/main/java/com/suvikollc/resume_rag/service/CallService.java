package com.suvikollc.resume_rag.service;

import com.suvikollc.resume_rag.dto.CallInitiationDTO;

public interface CallService {
	
	String startCall(CallInitiationDTO callDto);
	
	boolean isCallInProgress();

}
