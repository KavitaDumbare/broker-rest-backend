package com.radianbroker.controller;

import com.radianbroker.payload.request.HL7SentRequest;
import com.radianbroker.service.HL7SentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class HL7SentController {

	@Autowired
	HL7SentService hl7SentService;
	
	@PostMapping("/hl7-queued")
	public ResponseEntity<?> getAllHL7QueuedMessages(@RequestBody HL7SentRequest hl7QueuedRequest ) {
		try {
			return new ResponseEntity<>(hl7SentService.getAllHL7QueuedMessages(hl7QueuedRequest), HttpStatus.OK);
			
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping("/hl7-sent")
	public ResponseEntity<?> getAllHL7SentMessages(@RequestBody HL7SentRequest hl7SentRequest ) {
		try {
			return new ResponseEntity<>(hl7SentService.getAllHL7Sent(hl7SentRequest), HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/resend-hl7-sent-message")
	public  ResponseEntity<?> resendHL7SentMessage(@RequestParam String messageControlId) {
		try {
			return new ResponseEntity<>( hl7SentService.resendHL7SentMessage(messageControlId), HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/hl7-sent-message")
	public  ResponseEntity<?> getHL7SentMessage(@RequestParam String messageControlId) {

		Resource file;
		try {
			file = hl7SentService.getHL7SentMessage(messageControlId);
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
					.body(file);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/send-visit-hold-queue-message")
	public  ResponseEntity<?> sendVisitHoldQueueMessage(@RequestParam Long reportId) {
		try {
			return new ResponseEntity<>( hl7SentService.sendVisitHoldQueueMessage(reportId), HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

}
