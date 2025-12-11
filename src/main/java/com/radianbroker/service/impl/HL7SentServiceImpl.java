package com.radianbroker.service.impl;

import com.radianbroker.dto.HL7SentDTO;
import com.radianbroker.dto.VisitHoldQueueDTO;
import com.radianbroker.entity.*;
import com.radianbroker.exceptions.ResourceNotFoundException;
import com.radianbroker.payload.request.HL7SentRequest;
import com.radianbroker.repository.*;
import com.radianbroker.service.HL7SentService;
import com.radianbroker.specification.HL7SentSpecification;
import com.radianbroker.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HL7SentServiceImpl implements HL7SentService {

    @Autowired
    MipRepository mipRepository;

    @Autowired
    VisitRepository visitRepository;

    @Autowired
    RisRepository risRepository;

    @Autowired
    PatientRepository patientRepository;

    @Autowired
    HL7SentSpecification hl7SentSpecification;

    @Autowired
    ReportRepository reportRepository;

    @Override
    public Map<String, Object> getAllHL7QueuedMessages(HL7SentRequest hl7Request) throws Exception {

        if (hl7Request.getStartDate()!=null&&hl7Request.getEndDate()!=null)
        {
            DateUtils.verifyDateFormat(hl7Request.getStartDate());
            DateUtils.verifyDateFormat(hl7Request.getEndDate());
        }

        int page = hl7Request.getPage();
        int size = hl7Request.getSize() == 0 ? 5 : hl7Request.getSize();

        Pageable pagingSort = PageRequest.of(page, size);

        List<Long> mipIds = hl7Request.getMipIds();
        if (mipIds.isEmpty()) {
            mipIds = mipRepository.getMipIdsByRisId(hl7Request.getRisId());
        }

        Page<Visit> hl7QueuedListPages = visitRepository.getHl7QueuedList(hl7Request.getStartDate(),
                hl7Request.getEndDate(), hl7Request.getRisId(), mipIds, pagingSort);
        List<Visit> hl7QueuedMsgs = hl7QueuedListPages.getContent();

        List<VisitHoldQueueDTO> hl7QueuedDTOList = new ArrayList<VisitHoldQueueDTO>();
//		List<String> successAcknowledgmentCodes = HL7Sent.getSuccessAcknowledgmentCodes();

//		Long risId = null;
//		Long patientId = null;
//		boolean visitFound = false;
        for (Visit visit : hl7QueuedMsgs) {
            VisitHoldQueueDTO visitHoldQueueDTO = new VisitHoldQueueDTO();
//			Visit visit = visitRepository.getVisit(hl7Queued.getReportId());
//			if (visit != null) {
            visitHoldQueueDTO.setRisId(visit.getRisId());
            visitHoldQueueDTO.setReportId(visit.getReportId());
            visitHoldQueueDTO.setOrderNo(visit.getOrderNo());
            visitHoldQueueDTO.setVisitNo(visit.getVisitNo());
            visitHoldQueueDTO.setVisitStart(visit.getVisitStart().toString());
            visitHoldQueueDTO.setMip(visit.getMip());
            visitHoldQueueDTO.setMachine(visit.getMachine());
            visitHoldQueueDTO.setVisitStatus(visit.getState().toString());
//				risId = visit.getRisId();
//				patientId = visit.getPatientId();
//				visitFound = true;
//			}
//			if (!visitFound) {
//				throw new ResourceNotFoundException("Visit not found for reportId: " + hl7Queued.getReportId());
//			}
            Ris ris = risRepository.getRisById(visit.getRisId());
            Patient patient = patientRepository.findByPatientId(visit.getPatientId());
            visitHoldQueueDTO.setRisCode(ris.getRisCode());
            visitHoldQueueDTO.setPid(patient.getPid());
            visitHoldQueueDTO.setPatientName(patient.getLastName() + ", " + patient.getFirstName());

//			hl7QueuedDTO.setMessageControlId(hl7Queued.getMessageControlId());
//			hl7QueuedDTO.setAckCode(hl7Queued.getAckCode());
//			hl7QueuedDTO.setErrorComments(hl7Queued.getErrorComments());
//			hl7QueuedDTO.setAckResponse(hl7Queued.getAckResponse());
//			boolean reportSentSuccess = false;
//			if (hl7Queued.getAckCode() != null && hl7Queued.getErrorComments() == null) {
//				reportSentSuccess = successAcknowledgmentCodes.contains(hl7Queued.getAckCode().name());
//			}
//			hl7QueuedDTO.setReportSentSuccess(reportSentSuccess);
//			hl7QueuedDTO.setMesssageSentTime(hl7Queued.getLastModifiedDate());
            visitHoldQueueDTO.setVerificationDateTime(visit.getLastModifiedDate());
            hl7QueuedDTOList.add(visitHoldQueueDTO);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("hl7QueuedMessageList", hl7QueuedDTOList);
        response.put("currentPage", hl7QueuedListPages.getNumber());
        response.put("totalItems", hl7QueuedListPages.getTotalElements());
        response.put("totalPages", hl7QueuedListPages.getTotalPages());

        return response;
    }


    @Override
    public Map<String, Object> getAllHL7Sent(HL7SentRequest hl7SentRequest) {

        if (hl7SentRequest.getEndDate()!=null&&hl7SentRequest.getStartDate()!=null) {
            DateUtils.verifyDateFormat(hl7SentRequest.getStartDate());
            DateUtils.verifyDateFormat(hl7SentRequest.getEndDate());
        }
        int page = hl7SentRequest.getPage();
        int size = hl7SentRequest.getSize() == 0 ? 5 : hl7SentRequest.getSize();

        Pageable pagingSort = PageRequest.of(page, size);

        Page<HL7Sent> hl7SentListPages = hl7SentSpecification.getHL7Sent(hl7SentRequest, pagingSort);
        List<HL7Sent> hl7SentMsgs = hl7SentListPages.getContent();

        List<HL7SentDTO> hl7SentDTOList = new ArrayList<HL7SentDTO>();
        List<String> successAcknowledgmentCodes = HL7Sent.getSuccessAcknowledgmentCodes();

        Long risId = null;
        Long patientId = null;
        boolean visitFound = false;
        for (HL7Sent hl7Sent : hl7SentMsgs) {
            HL7SentDTO hl7SentDTO = new HL7SentDTO();

            Report report = reportRepository.getReport(hl7Sent.getReportId());
            if (report != null) {
                hl7SentDTO.setVisitNo(report.getVisitNo());
                hl7SentDTO.setVisitStart(report.getVisitStart().toString());
                hl7SentDTO.setMip(report.getMip());
                hl7SentDTO.setMachine(report.getMachine());
                hl7SentDTO.setVisitStatus(report.getState().toString());
                risId = report.getRisId();
                patientId = report.getPatientId();
                visitFound = true;
            }
            if (!visitFound) {
                throw new ResourceNotFoundException("Report not found for reportId: " + hl7Sent.getReportId());
            }
            Ris ris = risRepository.getRisById(risId);
            Patient patient = patientRepository.findByPatientId(patientId);
            hl7SentDTO.setRisCode(ris.getRisCode());
            hl7SentDTO.setPid(patient.getPid());
            hl7SentDTO.setPatientName(patient.getLastName() + ", " + patient.getFirstName());

            hl7SentDTO.setMessageControlId(hl7Sent.getMessageControlId());
            hl7SentDTO.setAckCode(hl7Sent.getAckCode());
            hl7SentDTO.setErrorComments(hl7Sent.getErrorComments());
            hl7SentDTO.setAckResponse(hl7Sent.getAckResponse());
            boolean reportSentSuccess = false;
            if (hl7Sent.getAckCode() != null && hl7Sent.getErrorComments() == null) {
                reportSentSuccess = successAcknowledgmentCodes.contains(hl7Sent.getAckCode().name());
            }
            hl7SentDTO.setReportSentSuccess(reportSentSuccess);
            hl7SentDTO.setMesssageSentTime(hl7Sent.getLastModifiedDate());
            hl7SentDTOList.add(hl7SentDTO);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("hl7OutMessageList", hl7SentDTOList);
        response.put("currentPage", hl7SentListPages.getNumber());
        response.put("totalItems", hl7SentListPages.getTotalElements());
        response.put("totalPages", hl7SentListPages.getTotalPages());

        return response;
    }
}
