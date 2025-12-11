package com.radianbroker.controller;

import com.radianbroker.payload.request.HL7QueueRequest;
import com.radianbroker.service.HL7QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class HL7QueueController {


	@Autowired
	HL7QueueService hl7QueueService;

	@PostMapping("/hl7-queue")
	public ResponseEntity<?> getAllHL7QueueMessages(@RequestBody HL7QueueRequest hl7QueueRequest) {
		try {
			return new ResponseEntity<>(hl7QueueService.getAllHL7QueueMessages(hl7QueueRequest), HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
	
	@GetMapping("/hl7-queue")
	public ResponseEntity<?> downloadHL7QueueMessage(@RequestParam String messageControlId) {
		Resource file;
		try {
			file= hl7QueueService.downloadHL7QueueMessage(messageControlId);
			 
			 return ResponseEntity.ok()
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
						.body(file);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
}
