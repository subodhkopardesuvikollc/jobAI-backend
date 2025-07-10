package com.suvikollc.resume_rag.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private KafkaProducer kafkaProducer;

    private static final String TOPIC = "resume-upload-events";
    private static final String TEST_FILE_NAME = "test-resume.pdf";

    @Test
    void testSendFileEvent_Success() {
        // Arrange
        when(kafkaTemplate.send(TOPIC, TEST_FILE_NAME)).thenReturn(null);

        // Act
        kafkaProducer.sendFileEvent(TEST_FILE_NAME);

        // Assert
        verify(kafkaTemplate).send(TOPIC, TEST_FILE_NAME);
    }

    @Test
    void testSendFileEvent_NullFileName() {
        // Arrange
        String nullFileName = null;

        // Act
        kafkaProducer.sendFileEvent(nullFileName);

        // Assert
        verify(kafkaTemplate).send(TOPIC, nullFileName);
    }

    @Test
    void testSendFileEvent_EmptyFileName() {
        // Arrange
        String emptyFileName = "";

        // Act
        kafkaProducer.sendFileEvent(emptyFileName);

        // Assert
        verify(kafkaTemplate).send(TOPIC, emptyFileName);
    }

    @Test
    void testSendFileEvent_MultipleFiles() {
        // Arrange
        String fileName1 = "resume1.pdf";
        String fileName2 = "resume2.docx";
        String fileName3 = "resume3.txt";

        // Act
        kafkaProducer.sendFileEvent(fileName1);
        kafkaProducer.sendFileEvent(fileName2);
        kafkaProducer.sendFileEvent(fileName3);

        // Assert
        verify(kafkaTemplate).send(TOPIC, fileName1);
        verify(kafkaTemplate).send(TOPIC, fileName2);
        verify(kafkaTemplate).send(TOPIC, fileName3);
        verify(kafkaTemplate, times(3)).send(eq(TOPIC), anyString());
    }

    @Test
    void testSendFileEvent_KafkaTemplateThrowsException() {
        // Arrange
        when(kafkaTemplate.send(TOPIC, TEST_FILE_NAME))
            .thenThrow(new RuntimeException("Kafka broker unavailable"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> kafkaProducer.sendFileEvent(TEST_FILE_NAME));
        
        verify(kafkaTemplate).send(TOPIC, TEST_FILE_NAME);
    }

    @Test
    void testSendFileEvent_LongFileName() {
        // Arrange
        String longFileName = "a".repeat(255) + ".pdf";

        // Act
        kafkaProducer.sendFileEvent(longFileName);

        // Assert
        verify(kafkaTemplate).send(TOPIC, longFileName);
    }

    @Test
    void testSendFileEvent_SpecialCharactersInFileName() {
        // Arrange
        String specialFileName = "résumé-with-spëcial-chärs_@#$%^&().pdf";

        // Act
        kafkaProducer.sendFileEvent(specialFileName);

        // Assert
        verify(kafkaTemplate).send(TOPIC, specialFileName);
    }

    @Test
    void testSendFileEvent_FileNameWithSpaces() {
        // Arrange
        String fileNameWithSpaces = "John Doe Resume 2024.pdf";

        // Act
        kafkaProducer.sendFileEvent(fileNameWithSpaces);

        // Assert
        verify(kafkaTemplate).send(TOPIC, fileNameWithSpaces);
    }

    @Test
    void testSendFileEvent_DifferentFileExtensions() {
        // Arrange
        String[] fileNames = {
            "resume.pdf",
            "resume.docx",
            "resume.txt",
            "resume.doc",
            "resume.rtf"
        };

        // Act
        for (String fileName : fileNames) {
            kafkaProducer.sendFileEvent(fileName);
        }

        // Assert
        for (String fileName : fileNames) {
            verify(kafkaTemplate).send(TOPIC, fileName);
        }
        verify(kafkaTemplate, times(5)).send(eq(TOPIC), anyString());
    }
}