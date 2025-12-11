package com.radianbroker.dto;



import com.radianbroker.entity.HL7Sent.AcknowledgmentCode;

import java.util.Date;

public class HL7SentDTO {

	public String visitNo;

	public String mip;

	public String machine;

	public String pid;

	public String patientName;

	public String visitStatus;

	public String messageControlId;

	private AcknowledgmentCode ackCode;

	private String errorComments;

	public String ackResponse;

	public boolean reportSentSuccess;

	public Date messsageSentTime;

	public String visitStart;
	public String risCode;
	
	public String getVisitNo() {
		return visitNo;
	}

	public void setVisitNo(String visitNo) {
		this.visitNo = visitNo;
	}

	public String getMip() {
		return mip;
	}

	public void setMip(String mip) {
		this.mip = mip;
	}

	public String getMachine() {
		return machine;
	}

	public void setMachine(String machine) {
		this.machine = machine;
	}

	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public String getPatientName() {
		return patientName;
	}

	public void setPatientName(String patientName) {
		this.patientName = patientName;
	}

	public String getVisitStatus() {
		return visitStatus;
	}

	public void setVisitStatus(String visitStatus) {
		this.visitStatus = visitStatus;
	}

	public AcknowledgmentCode getAckCode() {
		return ackCode;
	}

	public void setAckCode(AcknowledgmentCode ackCode) {
		this.ackCode = ackCode;
	}

	public String getErrorComments() {
		return errorComments;
	}

	public void setErrorComments(String errorComments) {
		this.errorComments = errorComments;
	}

	public String getMessageControlId() {
		return messageControlId;
	}

	public void setMessageControlId(String messageControlId) {
		this.messageControlId = messageControlId;
	}

	public String getAckResponse() {
		return ackResponse;
	}

	public void setAckResponse(String ackResponse) {
		this.ackResponse = ackResponse;
	}

	public boolean isReportSentSuccess() {
		return reportSentSuccess;
	}

	public void setReportSentSuccess(boolean reportSentSuccess) {
		this.reportSentSuccess = reportSentSuccess;
	}

	public Date getMesssageSentTime() {
		return messsageSentTime;
	}

	public void setMesssageSentTime(Date messsageSentTime) {
		this.messsageSentTime = messsageSentTime;
	}

	public String getRisCode() {
		return risCode;
	}

	public void setRisCode(String risCode) {
		this.risCode = risCode;
	}

	public String getVisitStart() {
		return visitStart;
	}

	public void setVisitStart(String visitStart) {
		this.visitStart = visitStart;
	}

	
}
