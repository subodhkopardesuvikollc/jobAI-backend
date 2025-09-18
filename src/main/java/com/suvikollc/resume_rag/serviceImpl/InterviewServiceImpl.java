package com.suvikollc.resume_rag.serviceImpl;

import java.util.List;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.suvikollc.resume_rag.entities.Interview;
import com.suvikollc.resume_rag.entities.Interview.Status;
import com.suvikollc.resume_rag.repository.InterviewRepository;
import com.suvikollc.resume_rag.repository.JdRepository;
import com.suvikollc.resume_rag.repository.ResumeRepository;
import com.suvikollc.resume_rag.service.InterviewService;

@Service
public class InterviewServiceImpl implements InterviewService {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private InterviewRepository interviewRepository;

	@Autowired
	private ResumeRepository resumeRepository;

	@Autowired
	private JdRepository jdRepository;

	@Override
	public List<String> getInterviewQuestions(String resumeId, String jdId) {

		var questions = interviewRepository.findByJdIdAndResumeId(new ObjectId(jdId), new ObjectId(resumeId));
		if (questions != null) {
			return questions.getQuestions();
		}
		throw new RuntimeException("No interview questions found for resumeId: " + resumeId + " and jdId: " + jdId);
	}

	public void saveScreeningQuestions(String resumeBlobName, String jdBlobName, List<String> questions) {

		var resume = resumeRepository.findByFileName(resumeBlobName);
		var jd = jdRepository.findByFileName(jdBlobName);
		if (resume == null || jd == null) {
			throw new RuntimeException("Jd or Resume not found with blob name: " + resumeBlobName);
		}
		Interview interview = interviewRepository.findByJdIdAndResumeId(jd.getId(), resume.getId());
		if (interview == null) {

			interview = new Interview();
		}
		interview.setJdId(jd.getId());
		interview.setResumeId(resume.getId());
		interview.setQuestions(questions);
		interviewRepository.save(interview);

		log.info("Saved {} interview questions for resume: {}, and jd: {} ", questions.size(), resumeBlobName,
				jdBlobName);
	}

	public void updateInterviewStatus(Status status, String resumeId, String jdId) {
		var interview = interviewRepository.findByJdIdAndResumeId(new ObjectId(jdId), new ObjectId(resumeId));
		if (interview == null) {
			throw new RuntimeException("No interview found for resumeId: " + resumeId + " and jdId: " + jdId);
		}
		interview.setStatus(status);
		interviewRepository.save(interview);
		log.info("Updated interview status to {} for resumeId: {} and jdId: {}", status, resumeId, jdId);
	}

	@Override
	public Interview getInterviewByIds(String resumeId, String jdId) {
		var interview = interviewRepository.findByJdIdAndResumeId(new ObjectId(jdId), new ObjectId(resumeId));
		if (interview == null) {
			throw new RuntimeException("No interview found for resumeId: " + resumeId + " and jdId: " + jdId);
		}
		return interview;
	}

}
