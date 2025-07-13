package com.suvikollc.resume_rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.azure.storage.file.share.ShareServiceClient;
import com.azure.storage.file.share.ShareServiceClientBuilder;

@SpringBootApplication()
@EnableMongoAuditing
public class ResumeRagApplication {
	private static final Logger log = LoggerFactory.getLogger(ResumeRagApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(ResumeRagApplication.class, args);
	}

	@Value("${spring.cloud.azure.storage.fileshare.connection-string}")
	private String connectionString;

	@Bean
	public ShareServiceClient shareServiceClient() {
		// Use the same builder logic that worked in your test
		return new ShareServiceClientBuilder().connectionString(connectionString).buildClient();
	}

}