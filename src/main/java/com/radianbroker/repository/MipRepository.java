package com.radianbroker.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.radianbroker.entity.Mip;
import com.radianbroker.projections.MipsProjection;


import java.util.List;

@Repository
public interface MipRepository extends JpaRepository<Mip, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM mips where ris_id=?1")
    List<Mip> findAllByRisId(long id);

    @Query(value = "SELECT mip_id FROM `mips` WHERE ris_id=?1", nativeQuery = true)
    List<Long> getMipIdsByRisId(Long risId);

	@Query(value = "SELECT m.mip_id as mipId, m.site_code as siteCode, m.name as name, m.ris_id as risId ,m.group_id as groupId FROM mips m WHERE m.status=true AND m.group_id=?1 ORDER BY m.site_code ASC", nativeQuery = true)
	List<MipsProjection> findForGroupAdminByGroupId(Long groupId);
}
