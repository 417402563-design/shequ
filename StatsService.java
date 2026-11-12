package cn.edu.sdua.shequ.service;

import cn.edu.sdua.shequ.entity.Bill;
import cn.edu.sdua.shequ.entity.Contract;
import cn.edu.sdua.shequ.entity.House;
import cn.edu.sdua.shequ.repository.BillRepository;
import cn.edu.sdua.shequ.repository.ContractRepository;
import cn.edu.sdua.shequ.repository.HouseRepository;
import cn.edu.sdua.shequ.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计分析服务（出租率统计、收入报表、热门区域分析）
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final HouseRepository houseRepository;
    private final ContractRepository contractRepository;
    private final BillRepository billRepository;
    private final TenantRepository tenantRepository;

    /** 总览指标 */
    public Map<String, Object> overview() {
        List<House> houses = houseRepository.findAll();
        long total = houses.size();
        long rented = houses.stream().filter(h -> "RENTED".equals(h.getStatus())).count();
        long onSale = houses.stream().filter(h -> "ON".equals(h.getStatus())).count();
        long offline = houses.stream().filter(h -> "OFF".equals(h.getStatus())).count();

        List<Bill> paidBills = billRepository.findByStatus("PAID");
        BigDecimal totalIncome = paidBills.stream().filter(b -> "RENT".equals(b.getType()))
                .map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        YearMonth thisMonth = YearMonth.now();
        BigDecimal monthIncome = paidBills.stream()
                .filter(b -> "RENT".equals(b.getType()) && b.getPayTime() != null
                        && YearMonth.from(b.getPayTime()).equals(thisMonth))
                .map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        long signedContracts = contractRepository.findByStatus("SIGNED").size();
        long unpaidBills = billRepository.findByStatus("UNPAID").size();

        Map<String, Object> map = new HashMap<>();
        map.put("houseTotal", total);
        map.put("houseRented", rented);
        map.put("houseOnSale", onSale);
        map.put("houseOffline", offline);
        // 出租率 = 已出租 /（总数-已下架）
        long online = total - offline;
        map.put("occupancyRate", online == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(rented * 100.0 / online).setScale(1, RoundingMode.HALF_UP));
        map.put("totalIncome", totalIncome);
        map.put("monthIncome", monthIncome);
        map.put("signedContracts", signedContracts);
        map.put("unpaidBills", unpaidBills);
        map.put("tenantTotal", tenantRepository.count());
        map.put("expiringContracts", expiringCount(30));
        return map;
    }

    /** 收入报表：近12个月租金收入 */
    public List<Map<String, Object>> monthlyIncome() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, BigDecimal> incomeByMonth = billRepository.findByStatus("PAID").stream()
                .filter(b -> "RENT".equals(b.getType()) && b.getPayTime() != null)
                .collect(Collectors.groupingBy(
                        b -> YearMonth.from(b.getPayTime()).format(fmt),
                        Collectors.mapping(Bill::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        List<Map<String, Object>> result = new ArrayList<>();
        YearMonth start = YearMonth.now().minusMonths(11);
        for (int i = 0; i < 12; i++) {
            YearMonth ym = start.plusMonths(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", ym.format(fmt));
            item.put("income", incomeByMonth.getOrDefault(ym.format(fmt), BigDecimal.ZERO));
            result.add(item);
        }
        return result;
    }

    /** 房源状态分布（出租率统计图数据） */
    public List<Map<String, Object>> houseStatusStats() {
        List<House> houses = houseRepository.findAll();
        Map<String, Long> count = houses.stream()
                .collect(Collectors.groupingBy(House::getStatus, Collectors.counting()));
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, String> labels = Map.of("ON", "空置可租", "RENTED", "已出租", "OFF", "已下架");
        labels.forEach((k, v) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", v);
            item.put("value", count.getOrDefault(k, 0L));
            list.add(item);
        });
        return list;
    }

    /** 热门区域分析：按区域的房源数、平均租金、总浏览量 */
    public List<Map<String, Object>> areaStats() {
        List<House> houses = houseRepository.findAll();
        Map<String, List<House>> byArea = houses.stream()
                .collect(Collectors.groupingBy(h -> h.getArea() == null ? "未知区域" : h.getArea()));
        List<Map<String, Object>> list = new ArrayList<>();
        byArea.forEach((area, hs) -> {
            long rented = hs.stream().filter(h -> "RENTED".equals(h.getStatus())).count();
            BigDecimal avgRent = hs.stream().map(House::getRent).filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(hs.size()), 0, RoundingMode.HALF_UP);
            int views = hs.stream().mapToInt(h -> h.getViewCount() == null ? 0 : h.getViewCount()).sum();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("area", area);
            item.put("houseCount", hs.size());
            item.put("rentedCount", rented);
            item.put("avgRent", avgRent);
            item.put("views", views);
            list.add(item);
        });
        // 热度 = 浏览量优先，其次出租量
        list.sort(Comparator.comparingInt((Map<String, Object> m) -> (int) m.get("views")).reversed());
        return list;
    }

    /** 30天内即将到期的生效合同数 */
    private long expiringCount(int days) {
        LocalDate today = LocalDate.now();
        return contractRepository.findByStatus("SIGNED").stream()
                .filter(c -> !c.getEndDate().isBefore(today) && !c.getEndDate().isAfter(today.plusDays(days)))
                .count();
    }
}
