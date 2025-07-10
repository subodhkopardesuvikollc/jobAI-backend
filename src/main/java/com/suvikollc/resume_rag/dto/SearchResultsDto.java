package com.suvikollc.resume_rag.dto;

public class SearchResultsDto {
	
	private String fileName;
	private String fileUrl;
	private Double score;

	public SearchResultsDto() {
	}

	public SearchResultsDto(String fileName, String fileUrl, Double score) {
		this.fileName = fileName;
		this.fileUrl = fileUrl;
		this.score = score;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileUrl() {
		return fileUrl;
	}

	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

}
