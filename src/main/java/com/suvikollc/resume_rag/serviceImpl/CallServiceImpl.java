package com.suvikollc.resume_rag.serviceImpl;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.models.AudioFormat;
import com.azure.communication.callautomation.models.CallInvite;
import com.azure.communication.callautomation.models.CreateCallOptions;
import com.azure.communication.callautomation.models.CreateCallResult;
import com.azure.communication.callautomation.models.MediaStreamingAudioChannel;
import com.azure.communication.callautomation.models.MediaStreamingOptions;
import com.azure.communication.callautomation.models.StreamingTransport;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.suvikollc.resume_rag.repository.ResumeRepository;
import com.suvikollc.resume_rag.service.CallService;
import com.suvikollc.resume_rag.service.ContactParserService;
import com.suvikollc.resume_rag.service.ResumeService;
import com.suvikollc.resume_rag.websockets.CallControlManager;

@Service
public class CallServiceImpl implements CallService {

	Logger log = LoggerFactory.getLogger(CallServiceImpl.class);

	@Value("${azure.communication.connection-string}")
	private String connectionString;

	@Autowired
	private CallAutomationClient callAutomationClient;

	private String CALLBACK_HOST = "https://thin-mails-shake.loca.lt";
	private String TRANSPORT_HOST = "wss://thin-mails-shake.loca.lt";

	@Value("${azure.communication.from-phone-number}")
	private String FROM_PHONE_NUMBER;

	@Autowired
	private ContactParserService contactParserService;

	@Autowired
	private ResumeRepository resumeRepository;

	@Autowired
	private ResumeService resumeService;

	@Autowired
	private CallControlManager callControlManager;

	@Override
	public String startCall(String resumeId) {

		var resume = resumeRepository.findById(new ObjectId(resumeId)).get();
		if (resume == null) {
			throw new RuntimeException("Resume not found with id: " + resumeId);
		}
		String toPhoneNumber = resumeService.getPhoneNo(resume.getBlobName());

		toPhoneNumber = contactParserService.extractPhoneNo(toPhoneNumber);

		CallInvite invite = new CallInvite(new PhoneNumberIdentifier(toPhoneNumber),
				new PhoneNumberIdentifier(FROM_PHONE_NUMBER));
		String CALLBACK_URI = CALLBACK_HOST + "/communication/call/callback";
		String TRANSPORT_URL = TRANSPORT_HOST + "/communication/call/acs/media?resumeId=" + resumeId;

		MediaStreamingOptions mediaOptions = new MediaStreamingOptions(MediaStreamingAudioChannel.MIXED,
				StreamingTransport.WEBSOCKET).setTransportUrl(TRANSPORT_URL).setEnableBidirectional(true)
				.setStartMediaStreaming(true).setAudioFormat(AudioFormat.PCM_24K_MONO);

		CreateCallOptions options = new CreateCallOptions(invite, CALLBACK_URI).setMediaStreamingOptions(mediaOptions);
		Response<CreateCallResult> result = callAutomationClient.createCallWithResponse(options, Context.NONE);
		callControlManager.setCallConnectionId(result.getValue().getCallConnectionProperties().getCallConnectionId());
		log.info("Call started with ID: {}", result.getValue().getCallConnectionProperties().getCallConnectionId());
		return result.getValue().getCallConnection().getCallProperties().getCallConnectionId();

	}

}
