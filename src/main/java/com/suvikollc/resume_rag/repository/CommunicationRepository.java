package com.suvikollc.resume_rag.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.suvikollc.resume_rag.entities.Communication;

public interface CommunicationRepository extends MongoRepository<Communication, ObjectId> {

}
