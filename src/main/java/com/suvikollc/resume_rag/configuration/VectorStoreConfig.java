package com.suvikollc.resume_rag.configuration;

import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.azure.AzureVectorStore;
import org.springframework.ai.vectorstore.azure.AzureVectorStore.MetadataField;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.search.documents.indexes.SearchIndexClient;
import com.azure.search.documents.indexes.SearchIndexClientBuilder;

@Configuration
public class VectorStoreConfig {

    @Bean
    SearchIndexClient searchIndexClient() {
		return new SearchIndexClientBuilder().endpoint(System.getenv("AZURE_URL"))
				.credential(new AzureKeyCredential(System.getenv("AZURE_SEARCH_API_KEY"))).buildClient();
	}

    @Bean
    @Primary
    VectorStore customVectorStore(SearchIndexClient searchIndexClient, EmbeddingModel embeddingModel) {
		return AzureVectorStore.builder(searchIndexClient, embeddingModel).initializeSchema(true)
				.indexName(System.getenv("AZURE_INDEX_NAME"))
				.filterMetadataFields(List.of(MetadataField.text("source_file"), MetadataField.text("section"),
						MetadataField.text("source")))
				.build();
	}

}
