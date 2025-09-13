package com.suvikollc.resume_rag.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

public record ResumeAnalysisResponseDTO(int overallMatchScore, String summary, List<SkillAssessment> skillAssessments,
		@JsonProperty(access = Access.WRITE_ONLY) List<String> screeningQuestions) {

	public record SkillAssessment(String skill, int score, String reason) {

	}

}
