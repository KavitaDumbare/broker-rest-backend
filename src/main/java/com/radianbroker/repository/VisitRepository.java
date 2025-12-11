package com.radianbroker.repository;


import com.radianbroker.entity.Visit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VisitRepository extends CrudRepository<Visit, Long>, JpaSpecificationExecutor<Visit> {

	@Query(name = "RadianAdmin.hl7Queued", countQuery ="  SELECT COUNT(*)  " +
			"			   FROM  " +
			"			      visits v " +
			"			   WHERE  " +
			"			      v.prime_study = true  " +
			"			      AND v.order_status = 'CM'  " +
			"				  AND v.state='H'" +
			"			      AND IFNULL((DATE(v.last_modified_date) >= ?1 AND DATE(v.last_modified_date) <= ?2),1)  " +
			"			      AND v.ris_id=?3  " +
			"			      AND v.mip_id IN (?4) ", nativeQuery = true)
	Page<Visit> getHl7QueuedList(String startDate, String endDate, Long risId, List<Long> mipIds,
								 Pageable pagingSort);

	@Query(value = "SELECT * FROM visits v WHERE v.visit_no = ?1", nativeQuery = true)
	Visit findByVisitNo(String visitNo);
}