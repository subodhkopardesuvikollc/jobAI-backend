package com.suvikollc.resume_rag.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommunicationDTO {
	
	public enum CommunicationType {
	    EMAIL("email-queue"),
	    PHONE("phone-queue"),
	    SMS("sms-queue"),
	    PUSH("push-queue");
	    
	    private final String queueName;
	    
	    CommunicationType(String queueName) {
	        this.queueName = queueName;
	    }
	    
	    public String getQueueName() {
	        return queueName;
	    }
	}

	
	private CommunicationType type;
	private Object payload;

}
