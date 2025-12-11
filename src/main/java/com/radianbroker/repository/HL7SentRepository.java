package com.radianbroker.repository;



import com.radianbroker.entity.HL7Sent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HL7SentRepository extends CrudRepository<HL7Sent, Long>, JpaSpecificationExecutor<HL7Sent> {

	HL7Sent findByMessageControlId(String messageControlId);
	
	@Query(value ="SELECT * FROM `hl7_sent` WHERE ris_id=?1 AND report_id= ?2 ORDER BY last_modified_date DESC LIMIT 1", nativeQuery = true)
	HL7Sent findByRisIdAndReportId(long risId, long reportId);
	
	@Query(name = "RadianAdmin.hl7Sent", countQuery =
			  " SELECT " + 
			  "	 COUNT(*) " + 
			  "  FROM " + 
			  "  	hl7_sent hsr, " + 
			  "	    reports r " + 
			  "  WHERE " + 
			  "	  hsr.report_id =  r.report_id " + 
			  "	  AND IFNULL((DATE(hsr.last_modified_date) >= ?1 AND DATE(hsr.last_modified_date) <= ?2),1) " +
			  "	  AND hsr.ris_id = ?3 " + 
			  "   AND r.mip_id IN (?4) ", nativeQuery = true)
	Page<HL7Sent> getHl7SentList(String startDate, String endDate, Long risId, List<Long> mipIds, Pageable pagingSort);

    // @Query(name = "RadianAdmin.hl7Queued", countQuery ="  SELECT COUNT(*)  " +
	// 		  "			   FROM  " + 
	// 		  "			      hl7_sent hsv, " + 
	// 		  "			      visits v " + 
	// 		  "			   WHERE  " + 
	// 		  "			      hsv.report_id =  v.report_id  " + 
	// 		  "			      AND DATE(hsv.last_modified_date) >= ?1 AND DATE(hsv.last_modified_date) <= ?2  " + 
	// 		  "			      AND hsv.ris_id=?3  " + 
	// 		  "			      AND v.mip_id IN (?4) ", nativeQuery = true)
	// Page<HL7Sent> getHl7QueuedList(String startDate, String endDate, Long risId, List<Long> mipIds,
	// 		Pageable pagingSort);
	
}
