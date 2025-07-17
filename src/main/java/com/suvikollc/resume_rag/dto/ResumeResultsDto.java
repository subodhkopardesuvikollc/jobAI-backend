package com.suvikollc.resume_rag.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class ResumeResultsDto {
	
	private String fileName;
	private String fileUrl;
	private Double score;

	

}
