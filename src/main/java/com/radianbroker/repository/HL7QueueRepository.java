package com.radianbroker.repository;


import com.radianbroker.entity.HL7Queue;
import com.radianbroker.projections.HL7QueueProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HL7QueueRepository extends CrudRepository<HL7Queue, Long>, JpaSpecificationExecutor<HL7Queue> {

	@Query(name = "RadianAdmin.hl7QueueMessages", countQuery =
			  " SELECT " + 
			  "	 COUNT(*) " + 
			  "  FROM " + 
			  "  	hl7_queue hq, " 
			  + " visits v " + 
			  "	WHERE "
			  + " hq.report_id = v.report_id "
			  + " AND IFNULL((DATE(hq.last_modified_date) >= ?1 AND DATE(hq.last_modified_date) <= ?2),1) "
			  + " AND hq.ris_id = ?3 AND v.mip_id IN (?4)  "
			  + " AND IFNULL((v.visit_no= ?5), 1) ORDER BY hq.id DESC", nativeQuery = true)
	Page<HL7QueueProjection> getHl7QueueMessagesList(String startDate, String endDate, Long risId, List<Long> mipIds,
													 String visitNo , Pageable pagingSort);

	HL7Queue findByMessageControlId(String messageControlId);
	
}
