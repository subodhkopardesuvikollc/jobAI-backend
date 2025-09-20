package com.suvikollc.resume_rag.websockets;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.azure.communication.callautomation.CallAutomationClient;

import lombok.Getter;

@Service
@Getter
public class CallControlManager {

	Logger log = LoggerFactory.getLogger(CallControlManager.class);

	@Autowired
	private CallAutomationClient callAutomationClient;

	private AtomicReference<String> callConnectionId = new AtomicReference<>();

	public void setCallConnectionId(String connectionId) {
		callConnectionId.set(connectionId);
	}

	public void clearCallConnectionId() {
		callConnectionId.set(null);
	}

	public void hangupCall() {
		if (callConnectionId.get() != null && !callConnectionId.get().isEmpty()) {
			try {
				callAutomationClient.getCallConnection(callConnectionId.get()).hangUp(true);
				log.info("Call hung up successfully for connection ID: {}", callConnectionId.get());
			} catch (Exception e) {
				log.error("Error hanging up call for connection ID {}: {}", callConnectionId.get(), e.getMessage());
			} finally {
				clearCallConnectionId();
			}
		} else {
			log.warn("No active call connection ID to hang up.");
		}
	}

}
