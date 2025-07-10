package com.suvikollc.resume_rag.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.file.share.ShareClient;
import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.ShareFileClient;
import com.azure.storage.file.share.ShareServiceClient;
import com.azure.storage.file.share.StorageFileInputStream;
import com.azure.storage.file.share.models.ShareFileItem;
import com.suvikollc.resume_rag.dto.SearchResultsDto;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ShareServiceClient shareServiceClient;

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private ShareClient shareClient;

    @Mock
    private ShareDirectoryClient shareDirectoryClient;

    @Mock
    private ShareFileClient shareFileClient;

    @Mock
    private PagedIterable<ShareFileItem> pagedIterable;

    @InjectMocks
    private IngestionSerivce ingestionService;

    private static final String SHARE_NAME = "test-share";
    private static final String TEST_FILE_NAME = "test-resume.pdf";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ingestionService, "shareName", SHARE_NAME);
        
        // Use lenient stubbing for common mocks to avoid unnecessary stubbing errors
        lenient().when(shareServiceClient.getShareClient(SHARE_NAME)).thenReturn(shareClient);
        lenient().when(shareClient.getRootDirectoryClient()).thenReturn(shareDirectoryClient);
        lenient().when(shareDirectoryClient.getFileClient(anyString())).thenReturn(shareFileClient);
    }

    @Test
    void testProcessDocument_Success() {
        // Arrange
        when(shareFileClient.exists()).thenReturn(true);
        
        InputStream mockInputStream = new ByteArrayInputStream("Test document content".getBytes());
        when(shareFileClient.openInputStream()).thenReturn((StorageFileInputStream) mockInputStream);
        
        // Act
        ingestionService.processDocument(TEST_FILE_NAME);
        
        // Assert
        verify(shareFileClient).exists();
        verify(shareFileClient).openInputStream();
        verify(vectorStore).accept(any(List.class));
    }

    @Test
    void testProcessDocument_FileNotFound() {
        // Arrange
        when(shareFileClient.exists()).thenReturn(false);
        
        // Act
        ingestionService.processDocument(TEST_FILE_NAME);
        
        // Assert
        verify(shareFileClient).exists();
        verify(shareFileClient, never()).openInputStream();
        verify(vectorStore, never()).accept(any(List.class));
    }

    @Test
    void testProcessDocument_Exception() {
        // Arrange
        when(shareFileClient.exists()).thenThrow(new RuntimeException("Azure connection failed"));
        
        // Act & Assert - Should not throw exception, just log error
        assertDoesNotThrow(() -> ingestionService.processDocument(TEST_FILE_NAME));
        
        verify(vectorStore, never()).accept(any(List.class));
    }

    @Test
    void testQueryDocument_Success() {
        // Arrange
        String query = "Java developer";
        Document doc1 = mock(Document.class);
        Document doc2 = mock(Document.class);
        
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("source_file", "resume1.pdf");
        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("source_file", "resume2.pdf");
        
        when(doc1.getMetadata()).thenReturn(metadata1);
        when(doc2.getMetadata()).thenReturn(metadata2);
        when(doc1.getScore()).thenReturn((double) 0.8f);
        when(doc2.getScore()).thenReturn((double) 0.7f);
        
        List<Document> mockResults = Arrays.asList(doc1, doc2);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(mockResults);
        
        // Act
        Map<String, Double> results = ingestionService.queryDocument(query);
        
        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.containsKey("resume1.pdf"));
        assertTrue(results.containsKey("resume2.pdf"));
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void testQueryDocument_NoSourceFile() {
        // Arrange
        String query = "Java developer";
        Document doc = mock(Document.class);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "fallback-source.pdf");
        
        when(doc.getMetadata()).thenReturn(metadata);
        when(doc.getScore()).thenReturn((double) 0.8f);
        
        List<Document> mockResults = Arrays.asList(doc);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(mockResults);
        
        // Act
        Map<String, Double> results = ingestionService.queryDocument(query);
        
        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.containsKey("fallback-source.pdf"));
    }

    @Test
    void testQueryDocument_Exception() {
        // Arrange
        String query = "Java developer";
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenThrow(new RuntimeException("Vector store error"));
        
        // Act
        Map<String, Double> results = ingestionService.queryDocument(query);
        
        // Assert
        assertNull(results);
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void testInitiatingDocumentLoad_Success() {
        // Arrange
        ShareFileItem fileItem1 = mock(ShareFileItem.class);
        ShareFileItem fileItem2 = mock(ShareFileItem.class);
        when(fileItem1.getName()).thenReturn("resume1.pdf");
        when(fileItem2.getName()).thenReturn("resume2.pdf");
        
        when(pagedIterable.iterator()).thenReturn(Arrays.asList(fileItem1, fileItem2).iterator());
        when(shareDirectoryClient.listFilesAndDirectories()).thenReturn(pagedIterable);
        
        // Act
        ingestionService.initiatingDocumentLoad();
        
        // Assert
        verify(kafkaProducer).sendFileEvent("resume1.pdf");
        verify(kafkaProducer).sendFileEvent("resume2.pdf");
        verify(shareDirectoryClient).listFilesAndDirectories();
    }

    @Test
    void testInitiatingDocumentLoad_NullFileName() {
        // Arrange
        ShareFileItem fileItem = mock(ShareFileItem.class);
        when(fileItem.getName()).thenReturn(null);
        
        when(pagedIterable.iterator()).thenReturn(Arrays.asList(fileItem).iterator());
        when(shareDirectoryClient.listFilesAndDirectories()).thenReturn(pagedIterable);
        
        // Act
        ingestionService.initiatingDocumentLoad();
        
        // Assert
        verify(kafkaProducer, never()).sendFileEvent(any());
    }

    @Test
    void testInitiatingDocumentLoad_Exception() {
        // Arrange
        when(shareDirectoryClient.listFilesAndDirectories()).thenThrow(new RuntimeException("Azure error"));
        
        // Act & Assert
        assertDoesNotThrow(() -> ingestionService.initiatingDocumentLoad());
        
        verify(kafkaProducer, never()).sendFileEvent(any());
    }

    @Test
    void testGetSharableUrl_Success() {
        // Arrange
        when(shareFileClient.exists()).thenReturn(true);
        when(shareFileClient.getFileUrl()).thenReturn("https://test.file.azure.com/file");
        when(shareFileClient.generateSas(any())).thenReturn("sasToken123");
        
        // Act
        String result = ingestionService.getSharableUrl(TEST_FILE_NAME);
        
        // Assert
        assertNotNull(result);
        assertEquals("https://test.file.azure.com/file?sasToken123", result);
        verify(shareFileClient).exists();
        verify(shareFileClient).generateSas(any());
    }

    @Test
    void testGetSharableUrl_FileNotFound() {
        // Arrange
        when(shareFileClient.exists()).thenReturn(false);
        
        // Act
        String result = ingestionService.getSharableUrl(TEST_FILE_NAME);
        
        // Assert
        assertNull(result);
        verify(shareFileClient).exists();
        verify(shareFileClient, never()).generateSas(any());
    }

    @Test
    void testGetSharableUrl_Exception() {
        // Arrange
        when(shareFileClient.exists()).thenThrow(new RuntimeException("Azure error"));
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> ingestionService.getSharableUrl(TEST_FILE_NAME));
        
        verify(shareFileClient, never()).generateSas(any());
    }

    @Test
    void testGetResults_Success() {
        // Arrange
        String query = "Java developer";
        Map<String, Double> mockQueryResults = new LinkedHashMap<>();
        mockQueryResults.put("resume1.pdf", 0.8);
        mockQueryResults.put("resume2.pdf", 0.7);
        
        // Mock the queryDocument method
        IngestionSerivce spyService = spy(ingestionService);
        doReturn(mockQueryResults).when(spyService).queryDocument(query);
        doReturn("https://test.com/resume1.pdf?sas=token1").when(spyService).getSharableUrl("resume1.pdf");
        doReturn("https://test.com/resume2.pdf?sas=token2").when(spyService).getSharableUrl("resume2.pdf");
        
        // Act
        List<SearchResultsDto> results = spyService.getResults(query);
        
        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        
        SearchResultsDto result1 = results.get(0);
        assertEquals("resume1.pdf", result1.getFileName());
        assertEquals("https://test.com/resume1.pdf?sas=token1", result1.getFileUrl());
        assertEquals(0.8, result1.getScore());
        
        SearchResultsDto result2 = results.get(1);
        assertEquals("resume2.pdf", result2.getFileName());
        assertEquals("https://test.com/resume2.pdf?sas=token2", result2.getFileUrl());
        assertEquals(0.7, result2.getScore());
    }

    @Test
    void testGetResults_NoResults() {
        // Arrange
        String query = "NonExistentSkill";
        
        IngestionSerivce spyService = spy(ingestionService);
        doReturn(null).when(spyService).queryDocument(query);
        
        // Act
        List<SearchResultsDto> results = spyService.getResults(query);
        
        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testGetResults_EmptyResults() {
        // Arrange
        String query = "EmptyQuery";
        
        IngestionSerivce spyService = spy(ingestionService);
        doReturn(new HashMap<>()).when(spyService).queryDocument(query);
        
        // Act
        List<SearchResultsDto> results = spyService.getResults(query);
        
        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testGetResults_Exception() {
        // Arrange
        String query = "Java developer";
        Map<String, Double> mockQueryResults = new LinkedHashMap<>();
        mockQueryResults.put("resume1.pdf", 0.8);
        
        IngestionSerivce spyService = spy(ingestionService);
        doReturn(mockQueryResults).when(spyService).queryDocument(query);
        doThrow(new RuntimeException("URL generation failed")).when(spyService).getSharableUrl("resume1.pdf");
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> spyService.getResults(query));
    }
}
