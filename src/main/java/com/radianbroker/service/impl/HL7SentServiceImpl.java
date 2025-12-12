package com.radianbroker.service.impl;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.app.Initiator;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v23.datatype.CM_ELD;
import ca.uhn.hl7v2.model.v23.datatype.ED;
import ca.uhn.hl7v2.model.v23.datatype.FT;
import ca.uhn.hl7v2.model.v23.datatype.XCN;
import ca.uhn.hl7v2.model.v23.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v23.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v23.group.ORU_R01_RESPONSE;
import ca.uhn.hl7v2.model.v23.message.ORM_O01;
import ca.uhn.hl7v2.model.v23.message.ORU_R01;
import ca.uhn.hl7v2.model.v23.segment.*;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.PipeParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.modelmapper.ModelMapper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import com.radianbroker.dto.*;
import com.radianbroker.entity.*;
import com.radianbroker.enums.MessageType;
import com.radianbroker.enums.Roles;
import com.radianbroker.enums.State;
import com.radianbroker.exceptions.HL7SendException;
import com.radianbroker.exceptions.ResourceNotFoundException;
import com.radianbroker.exceptions.StorageException;
import com.radianbroker.payload.request.HL7SentRequest;
import com.radianbroker.repository.*;
import com.radianbroker.service.*;
import com.radianbroker.specification.HL7SentSpecification;
import com.radianbroker.utils.DateUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import ca.uhn.hl7v2.model.v23.message.ACK;

@Service
public class HL7SentServiceImpl implements HL7SentService {
    public static final String NBSP_IN_UTF8 = "\u00a0";
    Logger logger = LoggerFactory.getLogger(HL7SentServiceImpl.class);
    String versionId = "2.3";
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

    @Autowired
    HL7SentRepository  hl7SentRepository;

    @Autowired
    DiagramService  diagramService;

    @Autowired
    RisService risService;

    @Autowired
    RisHL7ConfigRepository risHL7ConfigRepository;

    @Autowired
    HL7ReceivedService hl7ReceivedService;

    @Autowired
    RisUserService risUserService;


    private final int timeout = 60000;
    private final String HL7_SENT_DIR = "HL7_SENT";
    private final String HL7_QUEUE_DIR = "HL7_QUEUE";

    @Autowired
    FileSystemStorageService  fileSystemStorageService;

    HapiContext context;
    PipeParser parser;
    boolean useTls = false; // Should we use TLS/SSL?
    @Autowired
    public HL7SentServiceImpl() {
        CanonicalModelClassFactory mcf = new CanonicalModelClassFactory(versionId);
        context = new DefaultHapiContext();
        context.setModelClassFactory(mcf);
        context.getParserConfiguration().setValidating(false);
        parser = context.getPipeParser();
    }

    @Autowired
    UserRepository userRepository;

    @Autowired
    VisitReportQueueRedisService visitReportQueueRedisService;

    @Autowired
    VisitService visitService;

    @Value("${app.name}")
    private String appName;

    @Value("${spring.mail.username}")
    private String appSupportEmail;

    @Value("${app.support.emails}")
    private String supportTeamEmails;

    @Value("${company.name}")
    private String sendingApplication;

    @Autowired
    EmailService emailService;

    @Autowired
    AttachmentRepository attachmentRepository;

    @Autowired
    HL7RetryService  hl7RetryService;
    @Override
    public Message sendMessage(String host, int port, ORU_R01 oruR01) {

        try {
            Connection connection = context.newClient(host, port, useTls);
            Initiator initiator = connection.getInitiator();
            initiator.setTimeout(timeout, TimeUnit.MILLISECONDS);
            Message response = initiator.sendAndReceive(oruR01);
            connection.close();
            return response;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new HL7SendException(e.getMessage());
        }

    }

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

    @Override
    public HashMap<String, Object> resendHL7SentMessage(String messageControlId) throws Exception {
        // TODO Auto-generated method stub
        String fileName = messageControlId + ".hl7";

        HL7Sent hl7Sent = hl7SentRepository.findByMessageControlId(messageControlId);
        if (hl7Sent == null) {
            throw new ResourceNotFoundException("Record not found for messageControlId: " + messageControlId);
        }

        long reportId = hl7Sent.getReportId();

        VisitDTO visitDTO = null;
        Visit visit = null;
        Report report = null;
        Optional<Visit> visitOptional = visitRepository.findById(hl7Sent.getReportId());
        boolean foundInReports = false;
        if (visitOptional.isPresent()) {
            visit = visitOptional.get();
            ModelMapper modelMapper = new ModelMapper();
            visitDTO = modelMapper.map(visit, VisitDTO.class);
        }
        if (visitDTO == null) {
            Optional<Report> reportOptional = reportRepository.findById(hl7Sent.getReportId());
            if (reportOptional.isPresent()) {
                report = reportOptional.get();
                ModelMapper modelMapper = new ModelMapper();
                visitDTO = modelMapper.map(report, VisitDTO.class);
                foundInReports = true;
            }
        }
        if (visitDTO == null) {
            new ResourceNotFoundException("Visit not found for id: " + reportId);
        }
        Long patientId = visitDTO.getPatientId();
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found for id: " + patientId));

        diagramService.generateJpegForReportDiagrams(visitDTO.getReportId());
        List<Diagram> diagrams = diagramService.getAllUnVerifiedReportDiagrams(visitDTO.getReportId());

        Ris ris = risService.getRisById(visitDTO.getRisId());
        RisHL7Config risHL7Config= risHL7ConfigRepository.findByRisId(ris.getRisId());
        HL7Received hl7Received = hl7ReceivedService.findById(visitDTO.getUpdatedByMsgId());

        String hl7ReceivedMessageType = MessageType.ORM_O01.name();

        if (hl7Received != null) {
            if (hl7Received.getType().equals(hl7ReceivedMessageType)) {

                ObservingPractitioner mis1Practitioner = risUserService.getObservingPractitioner(ris.getRisId(),
                        visitDTO.getMis1());

                // ORU_R01 oruR01 = constructORUR01(messageControlId, hl7Received, ris, visit,
                // mis1Practitioner);
                ORU_R01 oruR01 = constructORUR01V2(messageControlId, hl7Received, ris, visitDTO, patient,
                        mis1Practitioner, diagrams);

                hl7Sent.setType(MessageType.ORU_R01.name());
                hl7Sent.setVersionId(versionId);

                // Write file here

                String directoryPath = fileSystemStorageService.writeHL7MessageToFile(ris.getRisCode(), HL7_SENT_DIR,
                        fileName, oruR01.encode());

                hl7Sent.setDirectoryPath(directoryPath);
                hl7Sent = hl7SentRepository.save(hl7Sent);

                Message ackResponse = sendMessage(risHL7Config.getHl7SendHost(), risHL7Config.getHl7SendPort(), oruR01);
                ACK ack = (ACK) ackResponse;
                String ackCode = getAckCode(ack);
                String errorComments = getErrorComments(ack);
                if (errorComments != null) {
                    sendMailFailedHL7Msg(errorComments, visitDTO.getOrderNo().toString());
                }
                HL7Sent.AcknowledgmentCode acknowledgmentCode = HL7Sent.AcknowledgmentCode.valueOf(ackCode);
                hl7Sent.setAckCode(acknowledgmentCode);
                hl7Sent.setErrorComments(errorComments);
                hl7Sent.setAckResponse(ackResponse.encode());

                if (foundInReports) {
                    report.setState(State.V);
                    reportRepository.save(report);
                    List<Report> nonPrimeReports = reportRepository
                            .findByOrderNoAndRisIdAndPrimeStudy(report.getOrderNo(), report.getRisId(), false);
                    if (nonPrimeReports.size() > 0) {
                        for (Report nonPrimeReport : nonPrimeReports) {
                            nonPrimeReport.setState(State.V);
                            nonPrimeReport.setLockedBy(null);
                        }
                        reportRepository.saveAll(nonPrimeReports);
                    }
                } else {
                    visit.setState(State.V);
                    visitRepository.save(visit);
                    List<Visit> nonPrimeVisits = visitRepository.findByOrderNoAndRisIdAndPrimeStudy(visit.getOrderNo(),
                            visit.getRisId(), false);
                    if (nonPrimeVisits.size() > 0) {
                        for (Visit nonPrimeVisit : nonPrimeVisits) {
                            nonPrimeVisit.setState(State.V);
                            nonPrimeVisit.setLockedBy(null);
                        }
                        visitRepository.saveAll(nonPrimeVisits);
                    }

                    movedVisitToReport(visit.getRisId(), visit.getOrderNo());
                }

                visitReportQueueRedisService.delete(visit.getRisId(), visit.getOrderNo());
                diagramService.updateDateVerifiedReportDiagrams(visit.getReportId());

                hl7Sent = hl7SentRepository.save(hl7Sent);

                List<String> successAcknowledgmentCodes = HL7Sent.getSuccessAcknowledgmentCodes();

                boolean reportSentSuccess = false;
                if (hl7Sent.getAckCode() != null) {
                    reportSentSuccess = successAcknowledgmentCodes.contains(hl7Sent.getAckCode().name());
                }
                HashMap<String, Object> response = new HashMap<String, Object>();
                response.put("success", true);
                response.put("messageControlId", messageControlId);
                response.put("reportSentSuccess", reportSentSuccess);
                response.put("messsageSentTime", hl7Sent.getLastModifiedDate());
                response.put("ackCode", hl7Sent.getAckCode().name());
                response.put("errorComments", hl7Sent.getErrorComments());
                response.put("ackResponse", ackResponse.encode());
                sendReportPaymentToAccounts(visit);
                return response;

            } else {
                throw new RuntimeException("Incorrect message type found:" + hl7Received.getType());
            }
        } else {
            throw new RuntimeException("Bad Request: HL7 In Message not found for ID:" + visit.getUpdatedByMsgId());
        }
    }

    @Async
    @Override
    public void movedVisitToReport(Long risId, Long orderNo) {
        // TODO Auto-generated method stub
        try {
            List<Visit> visits = visitRepository.findByRisIdAndOrderNo(risId, orderNo);
            for (Visit visit : visits) {
                Report reportFromDb = reportRepository.findByRisIdAndVisitNo(visit.getRisId(), visit.getVisitNo());
                if (reportFromDb != null) {
                    throw new Exception("Move Failed-Visit already exists in report table");
                }
                ModelMapper modelMapper = new ModelMapper();
                Report report = modelMapper.map(visit, Report.class);
                reportRepository.save(report);
                visitRepository.delete(visit);
            }
        } catch (Exception e) {
            String stacktrace = ExceptionUtils.getStackTrace(e);
            String subject = "Error: Move visit to report table for OrderNo: " + orderNo;

            StringBuilder exceptionMessage = new StringBuilder();
            exceptionMessage.append("Exception: ");
            exceptionMessage.append(System.getProperty("line.separator"));
            exceptionMessage.append(stacktrace);

            Map<String, Object> model = new HashMap<String, Object>();
            model.put("title", "Order No: " + orderNo);
            model.put("message", exceptionMessage);

            sendExceptionNotification(subject, model);
        }
    }

    @Async
    public void sendMailFailedHL7Msg(String error, String orderNo) throws Exception {
        String role = Roles.RADIANADMIN.name();
        List<User> users = userRepository.findAllByRole(role);
        StringBuilder str = new StringBuilder();
        for (User user : users) {
            str.append(user.getEmail() + ",");
        }
        System.out.println("str  " + str);
        emailService.sendFailedHL7MSG(error, str.toString(), orderNo);
        System.out.println("Execute method asynchronously. " + Thread.currentThread().getName());
    }

    public void sendExceptionNotification(String subject, Map<String, Object> model) {
        try {
            String template = "exception-notification-template.html";
            String[] to = supportTeamEmails.split(",");
            StringBuilder sb = new StringBuilder(appName);
            sb.append("-");
            sb.append(subject);

            model.put("appName", appName);

            Mail mail = new Mail(appSupportEmail, to, null, sb.toString(), null, model, template);
            System.out.println(mail.toString());
            emailService.sendExceptionNotificationEmail(mail);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }
    private String getAckCode(ACK ack) {
        // TODO Auto-generated method stub
        MSA msa = ack.getMSA();
        String ackCodeValue = msa.getMsa1_AcknowledgementCode().getValue();
        return ackCodeValue;
    }


    private String getErrorComments(ACK ack) {
        // TODO Auto-generated method stub
        ERR err = ack.getERR();
        CM_ELD[] errorCodeAndLocation = err.getErr1_ErrorCodeAndLocation();
        if (errorCodeAndLocation != null && errorCodeAndLocation.length > 0) {
            String codeIdentifyingErrorValue = errorCodeAndLocation[0].getCm_eld4_CodeIdentifyingError()
                    .getCe1_Identifier().getValue();
            if (codeIdentifyingErrorValue != null && !codeIdentifyingErrorValue.isEmpty()) {
                return codeIdentifyingErrorValue;
            } else {
                return null;
            }
        }
        return null;
    }

    private ORU_R01 constructORUR01V2(String messageControlId, HL7Received hl7Received, Ris ris, VisitDTO visitDto,
                                      Patient patient, ObservingPractitioner mis1Practitioner, List<Diagram> diagrams) throws Exception {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        String ormMessage = fileSystemStorageService.readHL7MessagesFromFile(hl7Received.getDirectoryPath());
        if (ormMessage == null) {
            throw new ResourceNotFoundException(
                    "Message not found at directory path: " + hl7Received.getDirectoryPath());
        }

        Message message = parser.parse(ormMessage);
        ORM_O01 ormO01 = (ORM_O01) message;

// 1. Initial message with MSH, PID, PV1 segments
        StringBuilder sb = new StringBuilder();
        sb.append(ormO01.getMSH().encode());
        sb.append("\r");
        sb.append(ormO01.getPATIENT().getPID().encode());
        sb.append("\r");
        sb.append(ormO01.getPATIENT().getPATIENT_VISIT().getPV1().encode());

// 2. Append OrderSegment[ORC] for Prime Visit
        String oruOrderSegment = getOrderSegment(visitDto.getVisitNo(), ormMessage);
        sb.append("\r");
        sb.append(oruOrderSegment);

// 3. Append OrderSegment[ORC] for Non-Prime Visit
        List<Visit> nonPrimeVisits = visitRepository.findByOrderNoAndRisIdAndPrimeStudy(visitDto.getOrderNo(),
                visitDto.getRisId(), false);
        for (Visit nonPrimeVisit : nonPrimeVisits) {
            HL7Received hl7Rcvd = hl7ReceivedService.findById(nonPrimeVisit.getUpdatedByMsgId());
            String ormMsg = fileSystemStorageService.readHL7MessagesFromFile(hl7Rcvd.getDirectoryPath());
            String oruOrderSgmnt = getOrderSegment(nonPrimeVisit.getVisitNo(), ormMsg);
            sb.append("\r");
            sb.append(oruOrderSgmnt);
        }

// 1.Convert ORM to ORU
        ORU_R01 oruR01 = new ORU_R01();
        oruR01.setParser(parser);
        oruR01.parse(sb.toString());

// 2.Update MSH segment fields
        String[] array = MessageType.ORU_R01.name().split("_");
        String messageType = array[0];
        String triggerEvent = array[1];
        oruR01.getMSH().getMsh3_SendingApplication().getNamespaceID().setValue(sendingApplication.toUpperCase());
        oruR01.getMSH().getMsh5_ReceivingApplication().getNamespaceID().setValue(ris.getName().toUpperCase());
        oruR01.getMSH().getMsh7_DateTimeOfMessage().getTs1_TimeOfAnEvent().setValue(timeStamp);
        oruR01.getMSH().getMsh9_MessageType().getCm_msg1_MessageType().setValue(messageType);
        oruR01.getMSH().getMsh10_MessageControlID().setValue(messageControlId);
        oruR01.getMSH().getMessageType().getTriggerEvent().setValue(triggerEvent);

// 3.Update PID segment
        oruR01.getRESPONSE().getPATIENT().getPID().getPid3_PatientIDInternalID(0).getCx1_ID()
                .setValue(patient.getPid());

        List<ORU_R01_ORDER_OBSERVATION> oruR01OrderObservationList = oruR01.getRESPONSE().getORDER_OBSERVATIONAll();

        String htmlReport = cleanHtmlReportDocument(visitDto.getReportText());

        String prevEnteringOrganization = null;
        String technicianId = null, technicianFamilyName = null, technicianGivenName = null;
        String placerOrderNumber = null;
        AtomicInteger obrCount = new AtomicInteger(1);
        for (ORU_R01_ORDER_OBSERVATION orderObservation : oruR01OrderObservationList) {
// 3.Update ORC segment fields
            if (!orderObservation.getORC().isEmpty()) {
                ORC orc = orderObservation.getORC();
                orc.getOrc5_OrderStatus().setValue("CM");
                String enteringOrganization = orc.getOrc17_EnteringOrganization().getCe1_Identifier().getValue();
                prevEnteringOrganization = enteringOrganization;

                if (orc.getOrc12_OrderingProvider().length > 0) {
                    XCN[] technician = orc.getOrc12_OrderingProvider();
                    technicianId = technician[0].getIDNumber().getValue();
                    technicianFamilyName = technician[0].getFamilyName().getValue();
                    technicianGivenName = technician[0].getGivenName().getValue();
                }
                placerOrderNumber = orc.getPlacerOrderNumber(0).getEi1_EntityIdentifier().getValue();
            }

// 4.Update OBR segment fields
            OBR obr = orderObservation.getOBR();

            String universalServiceIdentifierText = obr.getObr4_UniversalServiceIdentifier().getCe2_Text().getValue();
            obr.getObr1_SetIDObservationRequest().setValue(Integer.toString(obrCount.get()));
            obr.getObr4_UniversalServiceIdentifier().clear();

            obr.getObr25_ResultStatus().setValue("F");

            obr.getObr31_ReasonForStudy(0).getCe1_Identifier()
                    .setValue(prevEnteringOrganization + "-" + universalServiceIdentifierText);

            obr.getObr32_PrincipalResultInterpreter().getOPName().getCn1_IDNumber()
                    .setValue(mis1Practitioner.getUserName());
            obr.getObr32_PrincipalResultInterpreter().getStartDateTime().getTs1_TimeOfAnEvent()
                    .setValue(mis1Practitioner.getLastName());
            obr.getObr32_PrincipalResultInterpreter().getEndDateTime().getTs1_TimeOfAnEvent()
                    .setValue(mis1Practitioner.getFirstName());

            obr.getObr33_AssistantResultInterpreter(0).getOPName().getCn1_IDNumber()
                    .setValue(mis1Practitioner.getUserName());
            obr.getObr33_AssistantResultInterpreter(0).getStartDateTime().getTs1_TimeOfAnEvent()
                    .setValue(mis1Practitioner.getLastName());
            obr.getObr33_AssistantResultInterpreter(0).getEndDateTime().getTs1_TimeOfAnEvent()
                    .setValue(mis1Practitioner.getFirstName());

            obr.getObr34_Technician(0).getOPName().getCn1_IDNumber().setValue(technicianId);
            obr.getObr34_Technician(0).getStartDateTime().getTs1_TimeOfAnEvent().setValue(technicianFamilyName);
            obr.getObr34_Technician(0).getEndDateTime().getTs1_TimeOfAnEvent().setValue(technicianGivenName);

            obr.getObr35_Transcriptionist(0).getOPName().getCn1_IDNumber().setValue(technicianId);
            obr.getObr35_Transcriptionist(0).getStartDateTime().getTs1_TimeOfAnEvent().setValue(technicianFamilyName);
            obr.getObr35_Transcriptionist(0).getEndDateTime().getTs1_TimeOfAnEvent().setValue(technicianGivenName);

// 4.Update OBX segment fields
            OBX obx = orderObservation.getOBSERVATION().getOBX();

            obx.getObx1_SetIDOBX().setValue("1");
            obx.getObx2_ValueType().setValue("FT");
            obx.getObx3_ObservationIdentifier().getIdentifier().setValue(placerOrderNumber);

            FT ft = new FT(oruR01);
            ft.setValue(htmlReport);
            Varies value = obx.getObservationValue(0);
            value.setData(ft);

            obx.getObx11_ObservResultStatus().setValue("F");
            obx.getObx14_DateTimeOfTheObservation().getTs1_TimeOfAnEvent().setValue(timeStamp);

// 4.Update OBX segment fields
// Prepare draft file safely
            File draftFile = null;
            try {
                Attachment attachment = attachmentRepository.findByRisIdAndOrderNoAndName(ris.getRisId(),
                        visitDto.getOrderNo(), "DRAFT_WORKSHEET_" + visitDto.getOrderNo() + ".pdf");
                if (attachment != null) {
                    draftFile = fileSystemStorageService.getFile(attachment.getUrl());
                    if (!draftFile.exists()) {
                        throw new StorageException(
                                "DraftWorksheet file not found on disk for attachment ID: " + attachment.getId());
                    }
                }
            } catch (StorageException e) {
                logger.warn("Skipping draft worksheet: {}", e.getMessage());
                draftFile = null; // ensure it's null if any problem
            }

// Merge draft + diagrams
            File mergedFile = null;
            if ((draftFile != null && draftFile.exists()) || (diagrams != null && !diagrams.isEmpty())) {
                mergedFile = mergeDraftAndDiagramsToPdf(draftFile, diagrams);
            }

            if (mergedFile != null && mergedFile.exists()) {
                String base64String = fileSystemStorageService.encodeFileToBase64(mergedFile);

// Append a new OBSERVATION repetition safely at the end
                int newObsIndex = orderObservation.getOBSERVATIONReps();
                ORU_R01_OBSERVATION newObservation = orderObservation.insertOBSERVATION(newObsIndex);

                obx = newObservation.getOBX();

                obx.getSetIDOBX().setValue(Integer.toString(newObsIndex + 1)); // HL7 repeats start at 1
                obx.getValueType().setValue("ED");
                obx.getObservationIdentifier().getCe1_Identifier().setValue("U");
                obx.getObservationIdentifier().getCe2_Text().setValue("WORKSHEET " + 1);
                obx.getObservationIdentifier().getCe3_NameOfCodingSystem().setValue("L");
                obx.getObservationSubID().setValue("DATA^" + mergedFile.getName());

                ED ed = new ED(oruR01);
                ed.getDataSubtype().setValue("pdf");
                ed.getEncoding().setValue("Base64");
                ed.getData().setValue(base64String);

                Varies observationValue = obx.getObservationValue(0);
                observationValue.setData(ed);

// Clean up the temporary merged file after use
                if (!mergedFile.delete()) {
                    logger.warn("Failed to delete temporary worksheet file: {}", mergedFile.getAbsolutePath());
                }
            } else {
                logger.info("No merged file created, skipping OBSERVATION insertion for orderObservation at index: "
                        + obrCount.get());
            }

            obrCount.incrementAndGet();
            if (mergedFile != null && mergedFile.exists()) {
                mergedFile.delete();
            } else {
                logger.info("Failed to delete temporary worksheet file: ");
            }

        }
        return oruR01;
    }

    public File mergeDraftAndDiagramsToPdf(File draftPdf, List<Diagram> diagrams) throws Exception {
        com.itextpdf.text.Document document = new com.itextpdf.text.Document();
        File mergedFile = new File("merged_draft_and_diagrams.pdf");
        PdfCopy copy = new PdfCopy(document, new FileOutputStream(mergedFile));
        document.open();

// Add draft worksheet PDF pages
        if (draftPdf != null && draftPdf.exists()) {
            PdfReader reader = new PdfReader(draftPdf.getAbsolutePath());
            int n = reader.getNumberOfPages();
            for (int i = 1; i <= n; i++) {
                copy.addPage(copy.getImportedPage(reader, i));
            }
            reader.close();
        }

// Add diagram images, each on a new page
        for (Diagram diagram : diagrams) {
            File file = fileSystemStorageService.getFile(diagram.getFilePath());
            if (!file.exists()) {
                throw new StorageException("Diagram JPEG not found for diagramId: " + diagram.getDiagramId());
            }

            com.itextpdf.text.Document imgDoc = new com.itextpdf.text.Document();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(imgDoc, baos);
            imgDoc.open();

            Image img = Image.getInstance(file.getAbsolutePath());
            img.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
            img.setAlignment(Image.ALIGN_CENTER);
            imgDoc.add(img);
            imgDoc.close();

            PdfReader imgReader = new PdfReader(new ByteArrayInputStream(baos.toByteArray()));
            copy.addPage(copy.getImportedPage(imgReader, 1));
            imgReader.close();
        }

        document.close();
        return mergedFile;
    }
    public String cleanHtmlReportDocument(String htmlReport) {
        Document htmlDocument = Jsoup.parse(htmlReport);

        // 1. Change all <strong></strong> tag to <b></b> tag
        Elements strongElements = htmlDocument.select("strong");
        for (Element element : strongElements) {
            element.tagName("b");
        }

        // 2. Remove all whitespace bewteen <p> </p>
        for (Element element : htmlDocument.select("p")) {
            if (!element.hasText() || element.text().replaceAll(NBSP_IN_UTF8, "").trim().equals("")) {
                element.text("");
            }
        }

        // 3. Remove all <figure> tags
        for (Element element : htmlDocument.select("figure")) {
            // Create a new <p> element with the content of the <figure> element
            Element pElement = new Element("p").html(element.html());
            // Replace the <figure> element with the new <p> element
            element.replaceWith(pElement);
        }

        // 4. Remove all new lines
        String singleLineString = htmlDocument.html().replaceAll("\n", "");

        return singleLineString;
    }

    private String getOrderSegment(String visitNo, String ormMessage) throws HL7Exception {
        /**
         * Extract Order segment for given visit no only eg. ORC OBR [1..*] Note: If ORM
         * message have multiple OBR repetitions under ORC then while construct ORU
         * message only one OBR is required
         */

        ORU_R01 oruR01 = new ORU_R01();
        oruR01.setParser(parser);
        oruR01.parse(ormMessage);
        ORU_R01_RESPONSE oruR01RESPONSE = oruR01.getRESPONSE();

        StringBuilder sb = new StringBuilder();
        for (ORU_R01_ORDER_OBSERVATION orderOBSERVATION : oruR01RESPONSE.getORDER_OBSERVATIONAll()) {

            String placerOrderNumber = orderOBSERVATION.getORC().getPlacerOrderNumber(0).getEi1_EntityIdentifier()
                    .getValue();
            if (placerOrderNumber != null && !placerOrderNumber.isEmpty()) {
                if (visitNo.equals(placerOrderNumber)) {
                    sb.append(orderOBSERVATION.getORC().encode());
                    sb.append("\r");
                    sb.append(orderOBSERVATION.getOBR().encode());
                }
            } else {
//				if (orderOBSERVATION.getOBR().getPlacerOrderNumberReps() > 0) {
//					if (visitNo.equals(
//							orderOBSERVATION.getOBR().getPlacerOrderNumber()[0].getEi1_EntityIdentifier().getValue())) {
//						// OBR Repetition
//						sb.append("\r");
//						sb.append(orderOBSERVATION.getOBR().encode());
//					}
//				}
            }
        }
        return sb.toString();
    }
    @Async
    public void sendReportPaymentToAccounts(Visit visit) {


        visitService.sendReportPaymentToAccounts(visit.getRisId(), visit.getOrderNo());
    }

    @Override
    public Resource getHL7SentMessage(String messageControlId) throws Exception {
        // TODO Auto-generated method stub
        HL7Sent hl7Sent = hl7SentRepository.findByMessageControlId(messageControlId);
        if (hl7Sent == null) {
            throw new ResourceNotFoundException("Record not found for messageControlId: " + messageControlId);
        }

        return fileSystemStorageService.loadAsResource(hl7Sent.getDirectoryPath());
    }

    @Override
    public HashMap<String, Object> sendVisitHoldQueueMessage(Long reportId) throws Exception {
        // TODO Auto-generated method stub
        Visit visit = visitRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found for id: " + reportId));

        return createORUMessageAndSend(visit);
    }



    public HashMap<String, Object> createORUMessageAndSend(Visit visit) throws Exception {
        // TODO Auto-generated method stub
        try {
//			AtomicReference<jakarta.jms.Message> jmsMessage = new AtomicReference<>();
            Long patientId = visit.getPatientId();
            Patient patient = patientRepository.findById(visit.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found for id: " + patientId));

            Ris ris = risService.getRisById(visit.getRisId());
            RisHL7Config risHL7Config= risHL7ConfigRepository.findByRisId(ris.getRisId());
            HL7Received hl7Received = hl7ReceivedService.findById(visit.getUpdatedByMsgId());
            String hl7ReceivedMessageType = MessageType.ORM_O01.name();

            if (hl7Received != null) {
                if (hl7Received.getType().equals(hl7ReceivedMessageType)) {
                    UUID uuid = UUID.randomUUID();
                    String messageControlId = uuid.toString();
                    String fileName = messageControlId + ".hl7";

                    HL7Sent hl7Sent = hl7SentRepository.findByRisIdAndReportId(visit.getRisId(), visit.getReportId());

                    if (hl7Sent == null) {
                        hl7Sent = new HL7Sent();
                    } else {
                        // delete file by path
                        if (hl7Sent.getDirectoryPath() != null && !hl7Sent.getDirectoryPath().isEmpty()) {
                            fileSystemStorageService.deleteFile(hl7Sent.getDirectoryPath());
                        }
                    }
                    hl7Sent.setReportId(visit.getReportId());
                    hl7Sent.setRisId(visit.getRisId());
                    hl7Sent.setMessageControlId(messageControlId);
                    hl7Sent.setType(MessageType.ORU_R01.name());
                    hl7Sent.setVersionId(versionId);
                    hl7Sent = hl7SentRepository.save(hl7Sent);

                    ObservingPractitioner mis1Practitioner = risUserService.getObservingPractitioner(ris.getRisId(),
                            visit.getMis1());

                    // ORU_R01 oruR01 = constructORUR01(messageControlId, hl7Received, ris, visit,
                    // mis1Practitioner);
                    ModelMapper modelMapper = new ModelMapper();
                    VisitDTO visitDTO = modelMapper.map(visit, VisitDTO.class);

                    diagramService.generateJpegForReportDiagrams(visit.getReportId());
                    List<Diagram> diagrams = diagramService.getAllUnVerifiedReportDiagrams(visit.getReportId());

                    ORU_R01 oruR01 = constructORUR01V2(messageControlId, hl7Received, ris, visitDTO, patient,
                            mis1Practitioner, diagrams);

                    // write file here

                    String directoryPath = fileSystemStorageService.writeHL7MessageToFile(ris.getRisCode(),
                            HL7_SENT_DIR, fileName, oruR01.encode());
                    hl7Sent.setDirectoryPath(directoryPath);
                    hl7Sent = hl7SentRepository.save(hl7Sent); // save required here

                    // add entry in redis db
//					Date currentDate = Calendar.getInstance().getTime();
//					Date scheduledDate = org.apache.commons.lang3.time.DateUtils.addMilliseconds(currentDate,
//							INCOMING_QUEUE_HOLD_TIME);
//					UUID generationUUID = UUID.randomUUID();
//					String generationId = generationUUID.toString();

//					VisitReport visitReport = new VisitReport(generationId, messageControlId, visit.getRisId(),
//							visit.getReportId(), oruR01.encode(), currentDate, scheduledDate, ris.getHl7SendHost(),
//							ris.getHl7SendPort());
//
//					visitReportQueueRedisService.save(visitReport);

//					jmsTemplate.convertAndSend(INCOMING_QUEUE, visitReport, messagePostProcessor -> {
//
//						jmsMessage.set(messagePostProcessor);
//						return messagePostProcessor;
//					});

//					scheduler.schedule(new ReportHoldQueueTask(this, visitReport), scheduledDate);

                    Message ackResponse = hl7RetryService.sendORUHL7(hl7Sent.getId(), risHL7Config.getHl7SendHost(),
                            risHL7Config.getHl7SendPort(), oruR01);

                    // Need to fetch again so that no of retry count is updated in hl7RetryService
                    Optional<HL7Sent> updatedHL7Sent = hl7SentRepository.findById(hl7Sent.getId());
                    hl7Sent = updatedHL7Sent.get();

                    ACK ack = (ACK) ackResponse;
                    String ackCode = getAckCode(ack);
                    String errorComments = getErrorComments(ack);
                    HL7Sent.AcknowledgmentCode acknowledgmentCode = HL7Sent.AcknowledgmentCode.valueOf(ackCode);
                    if (errorComments != null) {
                        sendMailFailedHL7Msg(errorComments, visitDTO.getOrderNo().toString());
                    }
                    // 2.Update hl7_out_messages for ack, ackcode....
                    hl7Sent.setAckCode(acknowledgmentCode);
                    hl7Sent.setErrorComments(errorComments);
                    hl7Sent.setAckResponse(ackResponse.encode());
                    hl7Sent = hl7SentRepository.save(hl7Sent);

                    // 3.Update visit state to V
                    visit.setState(State.V);
                    visit.setLockedBy(null);
                    visit = visitRepository.save(visit);
                    List<Visit> nonPrimeVisits = visitRepository.findByOrderNoAndRisIdAndPrimeStudy(visit.getOrderNo(),
                            visit.getRisId(), false);
                    if (nonPrimeVisits.size() > 0) {
                        for (Visit nonPrimeVisit : nonPrimeVisits) {
                            nonPrimeVisit.setState(State.V);
                            nonPrimeVisit.setLockedBy(null);
                        }
                        visitRepository.saveAll(nonPrimeVisits);
                    }
                    List<String> successAcknowledgmentCodes = HL7Sent.getSuccessAcknowledgmentCodes();
                    boolean reportSentSuccess = false;
                    if (hl7Sent.getAckCode() != null) {
                        reportSentSuccess = successAcknowledgmentCodes.contains(hl7Sent.getAckCode().name());
                    }
                    HashMap<String, Object> response = new HashMap<String, Object>();
                    response.put("success", true);
                    response.put("messageControlId", messageControlId);
                    response.put("reportId", hl7Sent.getReportId());
                    response.put("reportSentSuccess", reportSentSuccess);
                    response.put("messsageSentTime", hl7Sent.getLastModifiedDate());
                    response.put("ackCode", hl7Sent.getAckCode().name());
                    response.put("errorComments", hl7Sent.getErrorComments());
                    response.put("ackResponse", hl7Sent.getAckCode());

                    // 6.Remove visit from queue
                    visitReportQueueRedisService.delete(visit.getRisId(), visit.getOrderNo());

                    // 7.move verified visit to reports
                    movedVisitToReport(visit.getRisId(), visit.getOrderNo());

                    // 8. update date verified for diagrams
                    diagramService.updateDateVerifiedReportDiagrams(visit.getReportId());

                    // 8.Sent visit info to account
                    sendReportPaymentToAccounts(visit);

                    hl7SentRepository.save(hl7Sent);
                    return response;
                } else {
                    throw new RuntimeException("Incorrect message type found:" + hl7Received.getType());
                }
            } else {
                throw new RuntimeException("Bad Request: HL7 In Message not found for ID:" + visit.getUpdatedByMsgId());
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            String stacktrace = ExceptionUtils.getStackTrace(e);
            String subject = "Exception caught while construct or sending ORU message";

            StringBuilder exceptionMessage = new StringBuilder();
            exceptionMessage.append("Exception: ");
            exceptionMessage.append(System.getProperty("line.separator"));
            exceptionMessage.append(stacktrace);

            Map<String, Object> model = new HashMap<String, Object>();
            model.put("title", "Visit No: " + visit.getVisitNo());
            model.put("message", exceptionMessage);

            sendExceptionNotification(subject, model);
            throw e;
        }
    }

 }
