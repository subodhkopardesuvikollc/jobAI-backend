package com.suvikollc.resume_rag.configuration;

import java.io.IOException;

import org.bson.types.ObjectId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {

	@Bean
	SimpleModule objectIdModule() {

		SimpleModule module = new SimpleModule();
		module.addSerializer(ObjectId.class, new ObjectIdSerializer());
		return module;

	}

	public class ObjectIdSerializer extends JsonSerializer<ObjectId> {

		@Override
		public void serialize(ObjectId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

			gen.writeString(value.toHexString());

		}

	}

}
