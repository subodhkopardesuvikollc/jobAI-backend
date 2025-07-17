package com.suvikollc.resume_rag.repository;

import java.util.List;

import com.suvikollc.resume_rag.entities.Resume;

public interface ResumeRepository extends FileRepository<Resume>{

	List<String> findAllByFileNameIn(List<String> resumeFileNames);

}
