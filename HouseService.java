package cn.edu.sdua.shequ.service;

import cn.edu.sdua.shequ.common.ApiException;
import cn.edu.sdua.shequ.entity.House;
import cn.edu.sdua.shequ.repository.HouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 房源管理服务（录入/编辑/下架/状态更新）
 */
@Service
@RequiredArgsConstructor
public class HouseService {

    private final HouseRepository houseRepository;

    public List<House> list(String status, String area, String keyword) {
        List<House> houses = houseRepository.findAll();
        if (status != null && !status.isBlank()) {
            houses = houses.stream().filter(h -> status.equals(h.getStatus())).toList();
        }
        if (area != null && !area.isBlank()) {
            houses = houses.stream().filter(h -> area.equals(h.getArea())).toList();
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            houses = houses.stream()
                    .filter(h -> (h.getTitle() != null && h.getTitle().contains(kw))
                            || (h.getAddress() != null && h.getAddress().contains(kw)))
                    .toList();
        }
        return houses;
    }

    public List<String> areas() {
        return houseRepository.findAll().stream()
                .map(House::getArea).distinct().sorted().toList();
    }

    public House detail(Long id) {
        House house = houseRepository.findById(id).orElseThrow(() -> new ApiException("房源不存在"));
        house.setViewCount(house.getViewCount() == null ? 1 : house.getViewCount() + 1);
        return houseRepository.save(house);
    }

    public House create(House house) {
        if (house.getStatus() == null || house.getStatus().isBlank()) house.setStatus("ON");
        if (house.getViewCount() == null) house.setViewCount(0);
        return houseRepository.save(house);
    }

    public House update(Long id, House req) {
        House house = houseRepository.findById(id).orElseThrow(() -> new ApiException("房源不存在"));
        copyEditable(house, req);
        return houseRepository.save(house);
    }

    /** 下架 */
    public House offShelf(Long id) {
        House house = houseRepository.findById(id).orElseThrow(() -> new ApiException("房源不存在"));
        if ("RENTED".equals(house.getStatus())) {
            throw new ApiException("房源已出租，无法下架，请先终止合同");
        }
        house.setStatus("OFF");
        return houseRepository.save(house);
    }

    /** 状态更新：ON空置可租 / RENTED已出租 / OFF已下架 */
    public House updateStatus(Long id, String status) {
        if (!List.of("ON", "RENTED", "OFF").contains(status)) {
            throw new ApiException("非法的房源状态：" + status);
        }
        House house = houseRepository.findById(id).orElseThrow(() -> new ApiException("房源不存在"));
        house.setStatus(status);
        return houseRepository.save(house);
    }

    public void delete(Long id) {
        House house = houseRepository.findById(id).orElseThrow(() -> new ApiException("房源不存在"));
        if ("RENTED".equals(house.getStatus())) {
            throw new ApiException("房源已出租，无法删除");
        }
        houseRepository.deleteById(id);
    }

    private void copyEditable(House house, House req) {
        house.setTitle(req.getTitle());
        house.setAddress(req.getAddress());
        house.setArea(req.getArea());
        house.setLayout(req.getLayout());
        house.setFloor(req.getFloor());
        house.setTotalFloors(req.getTotalFloors());
        house.setRent(req.getRent());
        house.setDeposit(req.getDeposit());
        house.setOwnerId(req.getOwnerId());
        house.setOwnerName(req.getOwnerName());
        house.setFacilities(req.getFacilities());
        house.setDescription(req.getDescription());
        if (req.getStatus() != null && !req.getStatus().isBlank()) house.setStatus(req.getStatus());
    }
}
