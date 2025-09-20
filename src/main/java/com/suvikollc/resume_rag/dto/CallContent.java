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
	public static class Utterance {

		private String speaker;
		private String text;

	}

	private List<Utterance> utterances = new ArrayList<>();

	public void addUtterence(String speaker, String text) {
		this.utterances.add(new Utterance(speaker, text));
	}

}
