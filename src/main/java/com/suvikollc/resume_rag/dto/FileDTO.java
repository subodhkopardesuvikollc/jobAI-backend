package com.suvikollc.resume_rag.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FileDTO<T> {
	
	private T file;
	
	private String fileUrl;

}
