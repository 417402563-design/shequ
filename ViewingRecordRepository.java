package cn.edu.sdua.shequ.repository;

import cn.edu.sdua.shequ.entity.ViewingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViewingRecordRepository extends JpaRepository<ViewingRecord, Long> {
    List<ViewingRecord> findByTenantIdOrderByViewTimeDesc(Long tenantId);
}
