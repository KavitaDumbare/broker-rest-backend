package com.radianbroker.dto;



import com.radianbroker.entity.HL7Queue.AcknowledgmentCode;

import java.util.Date;


public class HL7QueueDTO {
	public Long id;
	public String visitNo;
	public String mip;
	public String messageControlId;
	private AcknowledgmentCode ackCode;
	private String errorComments;
	public String ackResponse;
	public Date messsageSentTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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

	public String getMessageControlId() {
		return messageControlId;
	}

	public void setMessageControlId(String messageControlId) {
		this.messageControlId = messageControlId;
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

	public String getAckResponse() {
		return ackResponse;
	}

	public void setAckResponse(String ackResponse) {
		this.ackResponse = ackResponse;
	}

	public Date getMesssageSentTime() {
		return messsageSentTime;
	}

	public void setMesssageSentTime(Date messsageSentTime) {
		this.messsageSentTime = messsageSentTime;
	}

}
