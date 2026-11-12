package cn.edu.sdua.shequ.service;

import cn.edu.sdua.shequ.common.ApiException;
import cn.edu.sdua.shequ.entity.House;
import cn.edu.sdua.shequ.entity.Tenant;
import cn.edu.sdua.shequ.entity.ViewingRecord;
import cn.edu.sdua.shequ.repository.HouseRepository;
import cn.edu.sdua.shequ.repository.TenantRepository;
import cn.edu.sdua.shequ.repository.ViewingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 租客管理服务（信息登记、信用评估、看房记录）
 */
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final ViewingRecordRepository viewingRecordRepository;
    private final HouseRepository houseRepository;

    public List<Tenant> list(String keyword) {
        List<Tenant> tenants = tenantRepository.findAll();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            tenants = tenants.stream()
                    .filter(t -> (t.getName() != null && t.getName().contains(kw))
                            || (t.getPhone() != null && t.getPhone().contains(kw)))
                    .toList();
        }
        return tenants;
    }

    /** 新增/修改时依据信用分自动评定信用等级 */
    public Tenant save(Tenant tenant) {
        if (tenant.getCreditScore() != null) {
            int s = tenant.getCreditScore();
            if (s < 0 || s > 100) throw new ApiException("信用分需在0-100之间");
            tenant.setCreditLevel(s >= 90 ? "优秀" : s >= 75 ? "良好" : s >= 60 ? "一般" : "较差");
        }
        return tenantRepository.save(tenant);
    }

    public void delete(Long id) {
        viewingRecordRepository.findByTenantIdOrderByViewTimeDesc(id)
                .forEach(v -> viewingRecordRepository.delete(v));
        tenantRepository.deleteById(id);
    }

    public List<ViewingRecord> viewings(Long tenantId) {
        return viewingRecordRepository.findByTenantIdOrderByViewTimeDesc(tenantId);
    }

    /** 登记看房记录，同时累加房源浏览量 */
    public ViewingRecord addViewing(ViewingRecord record) {
        Tenant tenant = tenantRepository.findById(record.getTenantId())
                .orElseThrow(() -> new ApiException("租客不存在"));
        House house = houseRepository.findById(record.getHouseId())
                .orElseThrow(() -> new ApiException("房源不存在"));
        record.setTenantName(tenant.getName());
        record.setHouseTitle(house.getTitle());
        house.setViewCount(house.getViewCount() == null ? 1 : house.getViewCount() + 1);
        houseRepository.save(house);
        return viewingRecordRepository.save(record);
    }

    public void deleteViewing(Long id) {
        viewingRecordRepository.deleteById(id);
    }
}
