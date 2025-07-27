package com.suvikollc.resume_rag.serviceImpl;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.suvikollc.resume_rag.dto.FileDTO;
import com.suvikollc.resume_rag.entities.File;
import com.suvikollc.resume_rag.entities.Jd;
import com.suvikollc.resume_rag.entities.Resume;
import com.suvikollc.resume_rag.repository.FileRepository;
import com.suvikollc.resume_rag.repository.JdRepository;
import com.suvikollc.resume_rag.repository.ResumeRepository;
import com.suvikollc.resume_rag.service.FileService;

@Service
public class FileServiceImpl implements FileService {

	Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ResumeRepository resumeRepository;

	@Autowired
	private JdRepository jdRepository;

	@Value("${azure.storage.resume.container.name}")
	private String resumeContainerName;

	@Value("${azure.storage.jd.container.name}")
	private String jdContainerName;

	@Autowired
	private BlobServiceClient blobServiceClient;

	@Transactional
	public <T extends File> T uploadFile(MultipartFile file, Class<T> fileType) {
		String containerName = getContainerName(fileType);
		String blobName = file.getOriginalFilename();

		var blobClient = getBlobClient(blobName, containerName);

		try (InputStream inputStream = file.getInputStream()) {

			var savedFile = createFileInstance(fileType, file.getOriginalFilename(), blobName);
			savedFile = saveFile(savedFile);

			blobClient.upload(inputStream, file.getSize(), true);

			return savedFile;

		}

		catch (Exception e) {

			log.error("Error uploading file: deleting blob {}", blobName, e);

			blobClient.deleteIfExists();

			throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
		}

	}

	public BlobClient getBlobClient(String blobName, String containerName) {
		BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
		BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
		return blobClient;
	}

	private String getContainerName(Class<?> fileType) {

		if (fileType.equals(Resume.class)) {
			return resumeContainerName;
		} else if (fileType.equals(Jd.class)) {
			return jdContainerName;
		}
		throw new IllegalArgumentException("Unsupported file type: " + fileType.getSimpleName());

	}

	@SuppressWarnings("unchecked")
	private <T extends File> T createFileInstance(Class<T> fileType, String fileName, String blobName) {

		if (fileType.equals(Resume.class)) {
			return (T) new Resume(null, fileName, blobName, null, null);
		} else if (fileType.equals(Jd.class)) {
			return (T) new Jd(null, fileName, blobName, null, null, null);
		}
		throw new IllegalArgumentException("Unsupported file type: " + fileType.getSimpleName());

	}

	private <T extends File> T saveFile(T file) {
		if (file != null) {

			if (file instanceof Jd) {
				log.info("Saving Job Description file: {}", file.getFileName());
			} else if (file instanceof Resume) {
				log.info("Saving Resume file: {}", file.getFileName());
			}
			var fileRepository = getRepository(file);
			T existingFile = fileRepository.findByFileName(file.getFileName());
			T newFile = null;

			if (existingFile != null) {

				existingFile.setBlobName(file.getBlobName());

				newFile = fileRepository.save(existingFile);
			}

			else {
				newFile = fileRepository.save(file);
			}
			return newFile;
		}

		throw new RuntimeException("File cannot be null");
	}

	@SuppressWarnings("unchecked")
	private <T extends File> FileRepository<T> getRepository(T file) {
		if (file instanceof Jd) {
			return (FileRepository<T>) jdRepository;
		} else if (file instanceof Resume) {
			return (FileRepository<T>) resumeRepository;
		}

		throw new IllegalArgumentException("Unsupported file type: " + file.getClass().getSimpleName());
	}

	public String getSharableUrl(String blobName, String containerName) {

		log.info("Generating sharable URL for file: {}", blobName);
		try {

			var blobClient = getBlobClient(blobName, containerName);

			if (!blobClient.exists()) {
				log.error("File not found for generating sharable URL: {}", blobName);
				throw new RuntimeException("File not found: " + blobName);
			}
			OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(1);

			BlobSasPermission permissions = new BlobSasPermission().setReadPermission(true);
			BlobServiceSasSignatureValues signatureValues = new BlobServiceSasSignatureValues(expiryTime, permissions)
					.setContentDisposition("inline");
			String sasToken = blobClient.generateSas(signatureValues);

			return String.format("%s?%s", blobClient.getBlobUrl(), sasToken);
		} catch (Exception e) {
			log.error("Failed to generate sharable URL: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Error generating sharable URL", e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends File> List<T> getAllFiles(Class<T> fileType) {

		if (fileType.equals(Resume.class)) {
			return (List<T>) resumeRepository.findAll();
		} else if (fileType.equals(Jd.class)) {
			return (List<T>) jdRepository.findAll();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends File> List<FileDTO<T>> getAllFilesWithUrl(Class<T> fileType) {
		List<T> file = null;
		if (fileType.equals(Resume.class)) {
			file = (List<T>) resumeRepository.findAll();
		} else if (fileType.equals(Jd.class)) {
			file = (List<T>) jdRepository.findAll();
		}
		List<FileDTO<T>> fileDtos = file.stream().map(f -> {
			String sharableUrl = getSharableUrl(f.getBlobName(), getContainerName(fileType));
			return new FileDTO<>(f, sharableUrl);
		}).toList();

		return fileDtos;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends File> Page<FileDTO<T>> getAllFilesWithUrl(Class<T> fileType, Integer page, Integer size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<T> file = null;
		if (fileType.equals(Resume.class)) {
			file = (Page<T>) resumeRepository.findAll(pageable);
		} else if (fileType.equals(Jd.class)) {
			file = (Page<T>) jdRepository.findAll();
		}
		List<FileDTO<T>> fileDtos = file.getContent().stream().map(f -> {
			String sharableUrl = getSharableUrl(f.getBlobName(), getContainerName(fileType));
			return new FileDTO<>(f, sharableUrl);
		}).toList();
		PageImpl<FileDTO<T>> paginatedFiles = new PageImpl<>(fileDtos, pageable, file.getTotalElements());

		return paginatedFiles;
	}

	public String extractContent(InputStream stream) {
		try {
			StringBuilder contentBuilder = new StringBuilder();
			var resource = new InputStreamResource(stream);
			var tikaReader = new TikaDocumentReader(resource);
			tikaReader.get().stream().forEach(doc -> {
				contentBuilder.append(doc.getText());
			});

			return contentBuilder.toString();
		} catch (Exception e) {
			log.error("Error extracting content from stream: {}", e.getMessage());
			throw new RuntimeException("Error extracting content", e);
		}
	}

}
