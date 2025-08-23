package com.suvikollc.resume_rag.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.suvikollc.resume_rag.interceptor.ApiKeyInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Autowired
	private ApiKeyInterceptor apiKeyInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {

		registry.addInterceptor(apiKeyInterceptor).addPathPatterns("/**");
	}

}
