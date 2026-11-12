package cn.edu.sdua.shequ.repository;

import cn.edu.sdua.shequ.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByContractId(Long contractId);

    List<Bill> findByStatus(String status);

    List<Bill> findByTenantId(Long tenantId);
}
