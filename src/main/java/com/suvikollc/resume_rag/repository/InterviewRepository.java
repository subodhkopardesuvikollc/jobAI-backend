package com.suvikollc.resume_rag.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.suvikollc.resume_rag.entities.Interview;

@Repository
public interface InterviewRepository extends MongoRepository<Interview, ObjectId> {

	Interview findByJdIdAndResumeId(ObjectId jdId, ObjectId resumeId);

}
