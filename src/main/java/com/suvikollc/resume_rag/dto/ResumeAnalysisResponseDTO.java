package com.suvikollc.resume_rag.dto;

import java.util.List;

public record ResumeAnalysisResponseDTO(int overallMatchScore, String summary, List<SkillAssessment> skillAssessments) {

	public record SkillAssessment(String skill, int score, String reason) {

	}

}
