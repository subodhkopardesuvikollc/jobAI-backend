package com.suvikollc.resume_rag.service;

import org.bson.types.ObjectId;

public interface JDService {

	String generateKeywords(String jdBlobName);

	String generateSummary(String jdBlobName);

	String generateSummary(ObjectId jdId);

}
