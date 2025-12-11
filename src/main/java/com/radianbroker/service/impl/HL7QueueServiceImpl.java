package com.radianbroker.service.impl;


import com.radianbroker.dto.HL7QueueDTO;
import com.radianbroker.entity.HL7Queue;
import com.radianbroker.entity.Visit;
import com.radianbroker.exceptions.ResourceNotFoundException;
import com.radianbroker.payload.request.HL7QueueRequest;
import com.radianbroker.projections.HL7QueueProjection;
import com.radianbroker.repository.HL7QueueRepository;
import com.radianbroker.repository.ReportRepository;
import com.radianbroker.repository.VisitRepository;
import com.radianbroker.service.FileSystemStorageService;
import com.radianbroker.service.HL7QueueService;
import com.radianbroker.specification.HL7SentSpecification;
import com.radianbroker.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class HL7QueueServiceImpl implements HL7QueueService {

	@Autowired
	HL7SentSpecification hl7SentSpecification;

	@Autowired
	ReportRepository reportRepository;
	
	@Autowired
	VisitRepository visitRepository;
	
	@Autowired
	HL7QueueRepository hl7QueueRepository;
	
	@Autowired
	FileSystemStorageService fileSystemStorageService;
	
	@Override
	public Map<String, Object> getAllHL7QueueMessages(HL7QueueRequest hl7QueueRequest) throws ParseException {

		if(hl7QueueRequest.getEndDate()!=null&&hl7QueueRequest.getStartDate()!=null) {
			DateUtils.verifyDateFormat(hl7QueueRequest.getStartDate());
			DateUtils.verifyDateFormat(hl7QueueRequest.getEndDate());
		}
		int page = hl7QueueRequest.getPage();
		int size = hl7QueueRequest.getSize() == 0 ? 5 : hl7QueueRequest.getSize();

		Pageable pagingSort = PageRequest.of(page, size);

		Page<HL7QueueProjection> hl7QueueListPages = hl7SentSpecification.getHL7QueueMessages(hl7QueueRequest, pagingSort);
		List<HL7QueueProjection> hl7QueueMsgs = hl7QueueListPages.getContent();

		List<HL7QueueDTO> hl7QueueDTOList = new ArrayList<HL7QueueDTO>();

		for (HL7QueueProjection hl7QueueMsg : hl7QueueMsgs) {
			HL7QueueDTO hl7QueueDTO = new HL7QueueDTO();
			
			
			Visit visit=visitRepository.findByVisitNo(hl7QueueMsg.getVisitNo());
			String date = hl7QueueMsg.getLastModifiedDate();
			 Date messageSentDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(date);
			 hl7QueueDTO.setId(hl7QueueMsg.getId());
			hl7QueueDTO.setMip(visit.getMip());			
			hl7QueueDTO.setVisitNo(hl7QueueMsg.getVisitNo());
			hl7QueueDTO.setMessageControlId(hl7QueueMsg.getMessageControlId());
			hl7QueueDTO.setAckCode(hl7QueueMsg.getAckCode());
			hl7QueueDTO.setErrorComments(hl7QueueMsg.getErrorComments());
			hl7QueueDTO.setAckResponse(hl7QueueMsg.getAckResponse());
			hl7QueueDTO.setMesssageSentTime(messageSentDate);
			hl7QueueDTOList.add(hl7QueueDTO);
		}
		Map<String, Object> response = new HashMap<>();
		response.put("hl7QueueMessageList", hl7QueueDTOList);
		response.put("currentPage", hl7QueueListPages.getNumber());
		response.put("totalItems", hl7QueueListPages.getTotalElements());
		response.put("totalPages", hl7QueueListPages.getTotalPages());

		return response;
	}

	@Override
	public Resource downloadHL7QueueMessage(String messageControlId) throws Exception {
		HL7Queue hl7Queue = hl7QueueRepository.findByMessageControlId(messageControlId);
		if (hl7Queue == null) {
			throw new ResourceNotFoundException("Record not found for messageControlId: " + messageControlId);
		}

		return fileSystemStorageService.loadAsResource(hl7Queue.getDirectoryPath());
	}


}
