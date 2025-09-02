package com.suvikollc.resume_rag.serviceImpl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.suvikollc.resume_rag.entities.Resume.ResumeIndexStatus;
import com.suvikollc.resume_rag.repository.ResumeRepository;
import com.suvikollc.resume_rag.dto.ResumeAnalysisResponseDTO;
import com.suvikollc.resume_rag.service.FileService;
import com.suvikollc.resume_rag.service.ResumeService;

@Service
public class ResumeServiceImpl implements ResumeService {

	Logger log = LoggerFactory.getLogger(ResumeServiceImpl.class);

	@Autowired
	private VectorStore vectorStore;

	@Autowired
	private ResumeRepository resumeRepository;

	@Autowired
	private ChatClient chatClient;

	@Autowired
	private FileService fileService;

	@Value("${azure.storage.jd.container.name}")
	private String jdContainerName;

	@Value("${azure.storage.resume.container.name}")
	private String resumeContainerName;

	@Override
	public List<Document> retrieveRelavantCandidateWork(String resumeBlobName, String jobTitle, String jdKeywords) {

		String queryText = "Candidate's most impactful achievements and contributions relevant to a " + jobTitle
				+ " role, focusing on: " + jdKeywords;
		FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();

		Expression resumeAndSectionFilter = filterBuilder.and(filterBuilder.eq("source_file", resumeBlobName),
				filterBuilder.in("section", "experience", "professional experience", "projects",
						"projects (partial list)", "project experience", "projects overview", "portfolio",
						"employment history"))
				.build();

		try {
			SearchRequest request = SearchRequest.builder().query(queryText).filterExpression(resumeAndSectionFilter)
					.topK(3).build();
			List<Document> retrievedDocuments = vectorStore.similaritySearch(request);
			return retrievedDocuments;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Failed to retrieve relevant candidate work: " + e.getMessage());
		}
	}

	public boolean isResumeIndexed(String resumeBlobName) {
		var resume = resumeRepository.findByFileName(resumeBlobName);
		if (resume != null && resume.getIndexStatus() == ResumeIndexStatus.INDEXED) {
			return true;
		}
		return false;
	}

	@Override
	public void updatedResumeIndexStatus(String resumeBlobName, ResumeIndexStatus status) {

		var resume = resumeRepository.findByFileName(resumeBlobName);

		if (resume == null) {
			throw new RuntimeException("Resume not found with blob name: " + resumeBlobName);
		}
		if (resume.getIndexStatus() != status) {
			resume.setIndexStatus(status);
			resumeRepository.save(resume);
			log.info("Updated resume index status to {} for blob name: {}", status, resumeBlobName);
		}
	}

	@Override
	public ResumeAnalysisResponseDTO analyzeResume(String resumeBlobName, String jdBlobName) {

		String SYSTEM_PROMPT_TEMPLATE = """
				You are an expert technical recruiter with 20 years of experience.
				Your task is to provide a detailed analysis of the provided resume against the job description.
				Your analysis MUST be based *strictly* and *exclusively* on the information in the resume. Do not infer or hallucinate information.

				<instructions>
				First, identify the core skills and qualifications required by the job description.
				Then, for each of those skills, assess the candidate's proficiency based *only* on the resume content.
				Provide your final output in a structured JSON format that conforms to the following schema: {format}

				- **overallMatchScore**: A holistic score from 1 to 100 for the candidate's fit.
				- **summary**: A concise, two-paragraph summary explaining the overall score.
				- **skillAssessments**: A list of objects. For each key skill from the JD:
				    - **skill**: The name of the skill.
				    - **score**: A score from 1-100 indicating the candidate's proficiency based on the resume evidence.
				    - **reason**: A brief sentence explaining the score, citing specific projects or experiences from the resume. If a skill is not mentioned, the score should be low and the reason should state it is missing.
				</instructions>
				""";

		String USER_PROMPT_TEMPLATE = """
				<job_description>
				{jobDescription}
				</job_description>

				<resume>
				{resumeText}
				</resume>
				""";
		String resumeContent = fileService.extractContent(resumeBlobName, resumeContainerName);
		String jdContent = fileService.extractContent(jdBlobName, jdContainerName);

		try {
			var outputParser = new BeanOutputConverter<>(ResumeAnalysisResponseDTO.class);
			String format = outputParser.getFormat();
			String systemMessage = SYSTEM_PROMPT_TEMPLATE.replace("{format}", format);
			String userMessage = USER_PROMPT_TEMPLATE.replace("{jobDescription}", jdContent).replace("{resumeText}",
					resumeContent);

			log.info("Analyzing resume: {} of length {} characters, against JD: {} of length {} characters",
					resumeBlobName, resumeContent.length(), jdBlobName, jdContent.length());

			return chatClient.prompt().system(systemMessage).user(userMessage).call()
					.entity(ResumeAnalysisResponseDTO.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to analyze resume: " + e.getMessage());
		}

	}

}
