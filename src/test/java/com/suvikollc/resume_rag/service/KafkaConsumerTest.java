package com.suvikollc.resume_rag.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerTest {

    @Mock
    private IngestionSerivce ingestionService;

    @InjectMocks
    private KafkaConsumer kafkaConsumer;

    private static final String TEST_FILE_NAME = "test-resume.pdf";

    @Test
    void testListenForNewFile_Success() {
        // Arrange
        doNothing().when(ingestionService).processDocument(TEST_FILE_NAME);

        // Act
        kafkaConsumer.listenForNewFile(TEST_FILE_NAME);

        // Assert
        verify(ingestionService).processDocument(TEST_FILE_NAME);
    }

    @Test
    void testListenForNewFile_ServiceThrowsException() {
        // Arrange
        doThrow(new RuntimeException("Processing failed")).when(ingestionService).processDocument(TEST_FILE_NAME);

        // Act & Assert - Should not throw exception, just log error
        assertDoesNotThrow(() -> kafkaConsumer.listenForNewFile(TEST_FILE_NAME));

        verify(ingestionService).processDocument(TEST_FILE_NAME);
    }

    @Test
    void testListenForNewFile_NullFileName() {
        // Arrange
        String nullFileName = null;

        // Act
        kafkaConsumer.listenForNewFile(nullFileName);

        // Assert
        verify(ingestionService).processDocument(nullFileName);
    }

    @Test
    void testListenForNewFile_EmptyFileName() {
        // Arrange
        String emptyFileName = "";

        // Act
        kafkaConsumer.listenForNewFile(emptyFileName);

        // Assert
        verify(ingestionService).processDocument(emptyFileName);
    }

    @Test
    void testListenForNewFile_MultipleFiles() {
        // Arrange
        String fileName1 = "resume1.pdf";
        String fileName2 = "resume2.docx";
        String fileName3 = "resume3.txt";

        doNothing().when(ingestionService).processDocument(anyString());

        // Act
        kafkaConsumer.listenForNewFile(fileName1);
        kafkaConsumer.listenForNewFile(fileName2);
        kafkaConsumer.listenForNewFile(fileName3);

        // Assert
        verify(ingestionService).processDocument(fileName1);
        verify(ingestionService).processDocument(fileName2);
        verify(ingestionService).processDocument(fileName3);
        verify(ingestionService, times(3)).processDocument(anyString());
    }

    @Test
    void testListenForNewFile_ServiceProcessingFailure() {
        // Arrange
        RuntimeException serviceException = new RuntimeException("Vector store unavailable");
        doThrow(serviceException).when(ingestionService).processDocument(TEST_FILE_NAME);

        // Act
        kafkaConsumer.listenForNewFile(TEST_FILE_NAME);

        // Assert
        verify(ingestionService).processDocument(TEST_FILE_NAME);
        // Verify that the exception was handled gracefully (no re-throw)
        // The method should complete without throwing an exception
    }

    @Test
    void testListenForNewFile_LongFileName() {
        // Arrange
        String longFileName = "a".repeat(255) + ".pdf";
        doNothing().when(ingestionService).processDocument(longFileName);

        // Act
        kafkaConsumer.listenForNewFile(longFileName);

        // Assert
        verify(ingestionService).processDocument(longFileName);
    }

    @Test
    void testListenForNewFile_SpecialCharactersInFileName() {
        // Arrange
        String specialFileName = "résumé-with-spëcial-chärs_@#$%^&().pdf";
        doNothing().when(ingestionService).processDocument(specialFileName);

        // Act
        kafkaConsumer.listenForNewFile(specialFileName);

        // Assert
        verify(ingestionService).processDocument(specialFileName);
    }
}