package com.suvikollc.resume_rag.dto;

public class SearchRequestDto {

	String query;

	public SearchRequestDto() {
	}

	public SearchRequestDto(String query) {
		this.query = query;
	}

	public String query() {
		return query;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

}
