package com.radianbroker.service.impl;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import com.radianbroker.config.StorageProperties;
import com.radianbroker.exceptions.StorageException;
import com.radianbroker.service.FileSystemStorageService;


@Service
public class FileSystemStorageServiceImpl implements FileSystemStorageService{
	
	private final Path rootLocation;
	
	private String storageAlias;
	DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	@Autowired
	public FileSystemStorageServiceImpl(StorageProperties properties) {
		this.storageAlias = properties.getAlias();
		String OS = System.getProperty("os.name").toLowerCase();
		if(OS.equals("win") || OS.startsWith("windows")) {
			this.rootLocation = Paths.get(properties.getLocation());
		}else if(OS.equals("mac") || OS.equals("linux")) {
//			String homeDir = System.getProperty("user.home") + properties.getLocation();
//			this.rootLocation = Paths.get(homeDir); 
			this.rootLocation = Paths.get(properties.getLocation());
		} else {
			throw new StorageException("OS not supported: " + OS);
		}
	}
	@Override
	public Resource loadAsResource(String documentPath)throws Exception {
		try {
			Path file = load(documentPath);
			Resource resource = new UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource;
			} else {
				throw new StorageException("Could not read file at path: " + documentPath);
			}
		} catch (MalformedURLException e) {
			throw new StorageException("Could not read file at path: " + documentPath, e);
		}
	}

	@Override
	public Path load(String documentPath)throws Exception {
		return rootLocation.resolve(documentPath);
	}

	@Override
	public File getFile(String dirPath) throws Exception {
		String filePath = this.rootLocation.toAbsolutePath() + File.separator + dirPath;
		File file = new File(filePath);
		return file;
	}

	@Override
	public String writeHL7MessageToFile(String risCode, String directory, String fileName, String content) {
//		LocalDate currentdate = LocalDate.now();
//		String currentYear = Integer.toString(currentdate.getYear());
		String yearMonth = new SimpleDateFormat("yyyyMM").format(new Date());

		String dateString = dateFormat.format(new Date());

		Path dirPath = this.rootLocation.resolve(Paths.get(risCode, directory, yearMonth, dateString)).normalize()
				.toAbsolutePath();
		File dir = new File(dirPath.normalize().toString());
		if (!dir.exists()) {
			boolean directoryCreated = dir.mkdirs();
			if(!directoryCreated) {
				throw new StorageException("Could not create directory for path: " + dirPath.normalize().toString());
			}
		};

		Path filePath = this.rootLocation.resolve(Paths.get(risCode, directory, yearMonth, dateString, fileName));

		try (BufferedWriter writer = Files.newBufferedWriter(filePath, Charset.forName("UTF-8"))) {
			writer.write(content);
			return this.rootLocation.relativize(filePath).toString();
		} catch (IOException ex) {
			System.out.println("Could not write file on path: "+ filePath.normalize().toString());
			ex.printStackTrace();
		}
		return this.rootLocation.relativize(filePath).toString();
	}

	@Override
	public String readHL7MessagesFromFile(String filePath) throws Exception{
		Path path = load(filePath);
		File file = path.toFile();
		if (!file.exists()) {
			throw new StorageException("File not exists on path: " + path.normalize().toString());
		};
		InputStream is = new FileInputStream(file);
		is = new BufferedInputStream(is);
		Hl7InputStreamMessageStringIterator iter2 = new Hl7InputStreamMessageStringIterator(is);
		StringBuilder sb= new StringBuilder();
		while (iter2.hasNext()) {
			String next = iter2.next();
			sb.append(next);
		}
		return sb.toString();
	}

	@Override
	public String encodeFileToBase64(File file) throws Exception {
		try {
			byte[] fileContent = Files.readAllBytes(file.toPath());
			return Base64.getEncoder().encodeToString(fileContent);
		} catch (IOException e) {
			throw new IllegalStateException("could not read file " + file, e);
		}
	}

	@Override
	public boolean deleteFile(String filePath) {
		try {
			Path dirPath = this.rootLocation.resolve(Paths.get(filePath).normalize()).toAbsolutePath();
			boolean result = Files.deleteIfExists(dirPath);
			if (result) {
				return true;
			}
			return false;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
