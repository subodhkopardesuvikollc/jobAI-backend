package com.suvikollc.resume_rag.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CallContent {

	@Getter
	@Setter
	@AllArgsConstructor
	@NoArgsConstructor
	class Utterence {

		private String speaker;
		private String text;

	}

	private List<Utterence> utterences = new ArrayList<>();

	public void addUtterence(String speaker, String text) {
		this.utterences.add(new Utterence(speaker, text));
	}

}
