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
import com.suvikollc.resume_rag.dto.CallInitiationDTO;
import com.suvikollc.resume_rag.exceptions.CallInProgressException;
import com.suvikollc.resume_rag.repository.ResumeRepository;
import com.suvikollc.resume_rag.service.CallService;
import com.suvikollc.resume_rag.service.ResumeService;
import com.suvikollc.resume_rag.websockets.ACSSessionManager;
import com.suvikollc.resume_rag.websockets.CallControlManager;

import jakarta.annotation.PostConstruct;

@Service
public class CallServiceImpl implements CallService {

	Logger log = LoggerFactory.getLogger(CallServiceImpl.class);

	@Value("${azure.communication.connection-string}")
	private String connectionString;

	@Autowired
	private CallAutomationClient callAutomationClient;

	@Value("${app.url.host}")
	private String APP_HOST;

	private String CALLBACK_HOST;
	private String TRANSPORT_HOST;

	@Value("${azure.communication.from-phone-number}")
	private String FROM_PHONE_NUMBER;

	@Autowired
	private ResumeRepository resumeRepository;

	@Autowired
	private ResumeService resumeService;

	@Autowired
	private ACSSessionManager acsSessionManager;

	@Autowired
	private CallControlManager callControlManager;

	@PostConstruct
	public void init() {
		log.info("App Host: {}", APP_HOST);
		CALLBACK_HOST = APP_HOST;
		TRANSPORT_HOST = APP_HOST.replace("https", "wss");
		log.info("Callback Host: {}", CALLBACK_HOST);
		log.info("Transport Host: {}", TRANSPORT_HOST);
	}

	@Override
	public String startCall(CallInitiationDTO callDto) {

		if (isCallInProgress()) {
			throw new CallInProgressException("A call is already in progress. Please wait until it finishes.");
		}

		String resumeId = callDto.getResumeId();
		String jdId = callDto.getJdId();

		var resume = resumeRepository.findById(new ObjectId(resumeId)).get();
		if (resume == null) {
			throw new RuntimeException("Resume not found with id: " + resumeId);
		}
		String toPhoneNumber = resumeService.getPhoneNo(resume.getBlobName());

		CallInvite invite = new CallInvite(new PhoneNumberIdentifier(toPhoneNumber),
				new PhoneNumberIdentifier(FROM_PHONE_NUMBER));
		String CALLBACK_URI = CALLBACK_HOST + "/communication/call/callback";
		String TRANSPORT_URL = TRANSPORT_HOST + "/communication/call/acs/media?resumeId=" + resumeId + "&jdId=" + jdId;

		MediaStreamingOptions mediaOptions = new MediaStreamingOptions(MediaStreamingAudioChannel.MIXED,
				StreamingTransport.WEBSOCKET).setTransportUrl(TRANSPORT_URL).setEnableBidirectional(true)
				.setStartMediaStreaming(true).setAudioFormat(AudioFormat.PCM_24K_MONO);

		CreateCallOptions options = new CreateCallOptions(invite, CALLBACK_URI).setMediaStreamingOptions(mediaOptions);
		Response<CreateCallResult> result = callAutomationClient.createCallWithResponse(options, Context.NONE);
		callControlManager.setCallConnectionId(result.getValue().getCallConnectionProperties().getCallConnectionId());
		log.info("Call started with ID: {}", result.getValue().getCallConnectionProperties().getCallConnectionId());
		return result.getValue().getCallConnection().getCallProperties().getCallConnectionId();

	}

	@Override
	public boolean isCallInProgress() {

		var session = acsSessionManager.getAcsSession().get();
		if (session == null) {
			return false;
		}
		return session.isOpen();
	}

}
