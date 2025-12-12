package com.radianbroker.service;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v23.message.ORU_R01;
import com.radianbroker.payload.request.HL7SentRequest;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public interface HL7SentService {
    Object getAllHL7QueuedMessages(HL7SentRequest hl7QueuedRequest) throws Exception;

    Object getAllHL7Sent(HL7SentRequest hl7SentRequest);

    Object resendHL7SentMessage(String messageControlId) throws Exception;

    public Message sendMessage(String host, int port, ORU_R01 oruR01);

    public void movedVisitToReport(Long risId, Long orderNo);

    Resource getHL7SentMessage(String messageControlId) throws Exception;

    HashMap<String, Object> sendVisitHoldQueueMessage(Long reportId) throws Exception;

}
