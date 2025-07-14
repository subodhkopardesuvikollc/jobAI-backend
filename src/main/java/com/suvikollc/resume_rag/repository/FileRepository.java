package com.suvikollc.resume_rag.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.suvikollc.resume_rag.entities.File;

public interface FileRepository<T extends File> extends MongoRepository<T, ObjectId>{

	T findByFileName(String fileName);

}
