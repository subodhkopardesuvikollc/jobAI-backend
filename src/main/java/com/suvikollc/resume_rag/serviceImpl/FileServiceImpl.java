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
import com.suvikollc.resume_rag.repository.FileRepository;
import com.suvikollc.resume_rag.service.FileService;

@Service
public class FileServiceImpl implements FileService {

	Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private FileRepository fileRepository;
	
	@Value("${azure.storage.share.name}")
	private String shareName;

	@Value("${azure.storage.container.name}")
	private String containerName;


	@Autowired
	private ShareServiceClient shareServiceClient;
	
	@Autowired
	private BlobServiceClient blobServiceClient;

	@Transactional
	public File uploadFile(MultipartFile file) {

		BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);

		String blobName = file.getOriginalFilename();

		BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

		try (InputStream inputStream = file.getInputStream()) {

			File savedFile = saveFile(new File(null, file.getOriginalFilename(), blobName, null, null));

			blobClient.upload(inputStream, file.getSize(), true);

			return savedFile;

		}

		catch (Exception e) {

			log.error("Error uploading file: deleting blob {}", blobName, e);

			blobClient.deleteIfExists();

			throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
		}

	}

	private File saveFile(File file) {
		if (file != null) {
			File existingFile = fileRepository.findByFileName(file.getFileName());
			File newFile = null;
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
