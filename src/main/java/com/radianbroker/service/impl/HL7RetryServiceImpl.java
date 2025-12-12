package com.radianbroker.service.impl;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v23.message.ORU_R01;

import com.radianbroker.entity.HL7Sent;
import com.radianbroker.exceptions.HL7SendException;
import com.radianbroker.exceptions.ResourceNotFoundException;
import com.radianbroker.repository.HL7SentRepository;
import com.radianbroker.service.HL7RetryService;
import com.radianbroker.service.HL7SentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class HL7RetryServiceImpl implements HL7RetryService {

	@Autowired
	HL7SentService hl7SentService;
	
	@Autowired
	HL7SentRepository hl7SentRepository;

	@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 30000, multiplier = 2), value = { HL7SendException.class })
	@Override
	public Message sendORUHL7(Long hl7SentId, String hl7SendHost, Integer hl7SendPort, ORU_R01 oruR01) {
		
		HL7Sent hl7Sent = hl7SentRepository.findById(hl7SentId).orElseThrow(
				() -> new ResourceNotFoundException("HL7Sent not found for id: " + hl7SentId));
		int retryCount = hl7Sent.getRetryCount();
			
		try {
			System.out.println("sending..." + retryCount);
			Message message = hl7SentService.sendMessage(hl7SendHost, hl7SendPort, oruR01);
			return message;
        } catch (HL7SendException ex) {
        	System.out.println("in catch HL7SendException...");
        	retryCount++;
            hl7Sent.setRetryCount(retryCount);
            hl7SentRepository.save(hl7Sent);
            throw ex;
        }
	}

}
