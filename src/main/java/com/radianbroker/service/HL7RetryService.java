package com.radianbroker.service;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v23.message.ORU_R01;

public interface HL7RetryService {

	Message sendORUHL7(Long id, String hl7SendHost, Integer hl7SendPort, ORU_R01 oruR01);

}
