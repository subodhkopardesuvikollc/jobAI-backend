package com.suvikollc.resume_rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication()
@EnableMongoAuditing
public class ResumeRagApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResumeRagApplication.class, args);
	}


}