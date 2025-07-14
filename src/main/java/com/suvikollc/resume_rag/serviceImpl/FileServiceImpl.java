package com.suvikollc.resume_rag.serviceImpl;

import java.io.InputStream;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.file.share.ShareServiceClient;
import com.azure.storage.file.share.sas.ShareFileSasPermission;
import com.azure.storage.file.share.sas.ShareServiceSasSignatureValues;
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

	@Value("${azure.storage.share.name}")
	private String shareName;

	@Value("${azure.storage.resume.container.name}")
	private String resumeContainerName;

	@Value("${azure.storage.jd.container.name}")
	private String jdContainerName;

	@Autowired
	private ShareServiceClient shareServiceClient;

	@Autowired
	private BlobServiceClient blobServiceClient;

	@Transactional
	public <T extends File> T uploadFile(MultipartFile file, Class<T> fileType) {
		String containerName = getContainerName(fileType);
		BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);

		String blobName = file.getOriginalFilename();

		BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

		try (InputStream inputStream = file.getInputStream()) {

			var savedFile = createFileInstance(fileType, containerName, blobName);
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
			return (T) new Resume(null, fileName, blobName, null, null, null);
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

	public String getSharableUrl(String fileName) {

		log.info("Generating sharable URL for file: {}", fileName);
		try {
			var shareClient = shareServiceClient.getShareClient(shareName);
			var fileClient = shareClient.getRootDirectoryClient().getFileClient(fileName);
			if (!fileClient.exists()) {
				log.error("File not found for generating sharable URL: {}", fileName);
				return null;
			}
			OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(1);

			ShareFileSasPermission permissions = new ShareFileSasPermission().setReadPermission(true);
			ShareServiceSasSignatureValues signatureValues = new ShareServiceSasSignatureValues(expiryTime, permissions)
					.setContentDisposition("inline");
			String sasToken = fileClient.generateSas(signatureValues);

			return String.format("%s?%s", fileClient.getFileUrl(), sasToken);
		} catch (Exception e) {
			log.error("Failed to generate sharable URL: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Error generating sharable URL", e);
		}
	}

}
