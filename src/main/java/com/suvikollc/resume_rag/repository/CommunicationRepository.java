package com.suvikollc.resume_rag.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.suvikollc.resume_rag.dto.CommunicationDTO.CommunicationType;
import com.suvikollc.resume_rag.entities.Communication;

public interface CommunicationRepository extends MongoRepository<Communication, ObjectId> {

	List<Communication> findAllByJdIdAndResumeId(ObjectId objectId, ObjectId objectId2);

	Communication findByJdIdAndResumeIdAndType(ObjectId objectId, ObjectId objectId2, CommunicationType whatsapp);

}
