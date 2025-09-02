package com.suvikollc.resume_rag.interceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

	@Value("${custom.api.key}")
	private String VALID_API_KEY;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		String apiKey = request.getHeader("x-api-key");
		if (VALID_API_KEY.equals(apiKey)) {
			return true;
		} else {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().write("Unauthorized: Invalid API Key");
			return false;
		}

	}

}
