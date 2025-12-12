package com.radianbroker.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;

import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator;
import com.radianbroker.exceptions.StorageException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public interface FileSystemStorageService {

	Resource loadAsResource(String documentPath) throws Exception;
	Path load(String documentPath) throws Exception;

	File getFile(String dirPath) throws Exception;

	String writeHL7MessageToFile(String risCode, String hl7SentDir, String fileName, String encode);

	public String readHL7MessagesFromFile(String filePath) throws Exception;

	String encodeFileToBase64(File file) throws Exception;

    boolean deleteFile(String filePath);
}
