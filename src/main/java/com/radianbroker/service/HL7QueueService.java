package com.radianbroker.service;

import com.radianbroker.payload.request.HL7QueueRequest;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Map;

@Service
public interface HL7QueueService {
	Map<String, Object>  getAllHL7QueueMessages(HL7QueueRequest hl7QueueRequest) throws ParseException;

	Resource downloadHL7QueueMessage(String messageControlId) throws Exception;
}
