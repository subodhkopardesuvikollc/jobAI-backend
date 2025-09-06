package com.suvikollc.resume_rag.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.models.CallInvite;
import com.azure.communication.callautomation.models.CreateCallResult;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.suvikollc.resume_rag.service.CallService;

@Service
public class CallServiceImpl implements CallService {

	Logger log = LoggerFactory.getLogger(CallServiceImpl.class);

	@Value("${azure.communication.connection-string}")
	private String connectionString;

	@Autowired
	private CallAutomationClient callAutomationClient;

	private String CALLBACK_HOST = "https://busy-aliens-wave.loca.lt";

	@Value("${azure.communication.from-phone-number}")
	private String FROM_PHONE_NUMBER;

	@Override
	public String startCall(String toPhoneNumber) {

		CallInvite invite = new CallInvite(new PhoneNumberIdentifier(toPhoneNumber),
				new PhoneNumberIdentifier(FROM_PHONE_NUMBER));
		String CALLBACK_URI = CALLBACK_HOST + "/communication/callback";
		CreateCallResult result = callAutomationClient.createCall(invite, CALLBACK_URI);
		log.info("Call started with ID: {}", result.getCallConnection().getCallProperties().getCallConnectionId());
		return result.getCallConnection().getCallProperties().getCallConnectionId();

	}

}
