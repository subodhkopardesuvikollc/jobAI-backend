package com.suvikollc.resume_rag.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.communication.callautomation.CallAutomationClient;
import com.azure.communication.callautomation.CallAutomationClientBuilder;

@Configuration
public class ACSConfig {

	@Value("${azure.communication.connection-string}")
	private String connectionString;
	
	@Bean
	CallAutomationClient callAutomationClient() {
		return new CallAutomationClientBuilder()
				.connectionString(connectionString)
				.buildClient();
	}
}
