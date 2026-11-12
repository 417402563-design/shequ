package cn.edu.sdua.shequ.init;

import cn.edu.sdua.shequ.entity.*;
import cn.edu.sdua.shequ.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 首次启动时初始化演示数据（数据库已有数据则跳过）
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final HouseRepository houseRepository;
    private final TenantRepository tenantRepository;
    private final ViewingRecordRepository viewingRecordRepository;
    private final ContractRepository contractRepository;
    private final BillRepository billRepository;
    private final NotificationRepository notificationRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        // ============ 用户账号（初始密码均为 123456）============
        User agent = user("agent1", "张伟", "AGENT", "13908001111", "agent1@shequ.com");
        User ld1 = user("landlord1", "李芳", "LANDLORD", "13908002222", "lf@shequ.com");
        User ld2 = user("landlord2", "王建国", "LANDLORD", "13908003333", "wjg@shequ.com");
        User ld3 = user("landlord3", "陈桂芳", "LANDLORD", "13908004444", "cgf@shequ.com");

        // ============ 房源 ============
        House h1 = house("高新南区 天府三街 精装套二", "高新区天府三街69号1栋", "高新区", "两室一厅", 12, 28,
                3200, 3200, "RENTED", ld1, "空调,冰箱,洗衣机,宽带,热水器", "地铁一号线旁，拎包入住，采光极佳。", 86);
        House h2 = house("锦江区 春熙路 温馨一居", "锦江区中纱街88号", "锦江区", "一室一厅", 8, 22,
                2600, 2600, "RENTED", ld2, "空调,洗衣机,宽带", "市中心商圈，交通购物便利。", 132);
        House h3 = house("武侯区 桐梓林 品质套三", "武侯区人民南路四段12号", "武侯区", "三室两厅", 5, 18,
                4500, 4500, "ON", ld1, "空调,冰箱,洗衣机,宽带,燃气地暖", "绿化小区，安静宜居，近倪家坪地铁。", 45);
        House h4 = house("高新区 金融城 江景套四", "高新区交子大道233号", "高新区", "四室两厅", 26, 33,
                6800, 6800, "ON", ld3, "中央空调,冰箱,洗衣机,宽带,智能家居", "双子塔景观，高端公寓。", 68);
        House h5 = house("青羊区 光华村 老式套二", "青羊区光华西街22号", "青羊区", "两室一厅", 3, 6,
                1800, 1800, "ON", ld2, "空调,洗衣机", "成熟社区，生活气息浓厚。", 21);
        House h6 = house("成华区 建设路 电梯公寓", "成华区建设路10号", "成华区", "一室一厅", 15, 30,
                2200, 2200, "ON", ld3, "空调,冰箱,洗衣机,宽带", "东郊记忆旁，年轻人聚集地。", 57);
        House h7 = house("锦江区 攀成钢 豪宅套三", "锦江区天府望江东路99号", "锦江区", "三室两厅", 9, 26,
                5800, 5800, "OFF", ld1, "中央空调,冰箱,洗衣机,宽带,车位", "业主自住装修，暂时下架。", 30);
        House h8 = house("高新区 大源 合租单间", "高新区大源北街16号", "高新区", "单间", 7, 18,
                1200, 1200, "ON", ld3, "空调,洗衣机,宽带", "地铁5号线，合租公区整洁。", 95);

        // ============ 租客档案 ============
        Tenant t1 = tenant("刘洋", "男", "510100199505121234", "13600001111", 31, "软件工程师", 92, "按时缴租，爱干净");
        Tenant t2 = tenant("赵敏", "女", "510100199803034321", "13600002222", 28, "教师", 85, "稳定工作，作息规律");
        Tenant t3 = tenant("孙浩", "男", "510100199311117777", "13600003333", 33, "销售经理", 70, "曾有一次逾期缴租记录");
        Tenant t4 = tenant("周雨", "女", "510100200001018888", "13600004444", 26, "设计师", 88, "爱干净，沟通顺畅");
        Tenant t5 = tenant("吴斌", "男", "510100199007075555", "13600005555", 36, "自由职业", 55, "收入不稳定，需谨慎评估");

        // ============ 看房记录 ============
        viewing(t1, h1, LocalDateTime.now().minusDays(160), "户型满意，价格合适，决定承租", 1);
        viewing(t2, h2, LocalDateTime.now().minusDays(50), "位置喜欢，稍显嘈杂，可接受", 1);
        viewing(t3, h3, LocalDateTime.now().minusDays(12), "租金略高，考虑中", 0);
        viewing(t4, h8, LocalDateTime.now().minusDays(8), "单间性价比高，想约二刷", 1);
        viewing(t4, h4, LocalDateTime.now().minusDays(6), "超出预算", 0);
        viewing(t5, h6, LocalDateTime.now().minusDays(3), "先看看周边对比", 0);

        // ============ 合同与账单 ============
        // 生效中：c1 高新精装套二 - 刘洋（季付，去年签约）
        Contract c1 = contract("HT20260520001", h1, t1, ld1, agent,
                LocalDate.now().minusMonths(5), LocalDate.now().plusMonths(7), 3, "SIGNED");
        c1.setSignTime(LocalDate.now().minusMonths(5).atTime(10, 30));
        c1.setSignMethod("ONLINE");
        billsFor(c1, 2); // 前2期已缴，后2期待缴

        // 即将到期（30天内）：c2 春熙路一居 - 赵敏（月付）
        Contract c2 = contract("HT20251115002", h2, t2, ld2, agent,
                LocalDate.now().minusMonths(11), LocalDate.now().plusDays(18), 1, "SIGNED");
        c2.setSignTime(LocalDate.now().minusMonths(11).atTime(14, 0));
        c2.setSignMethod("ONLINE");
        billsFor(c2, 10); // 最后1期租金逾期未缴

        // 本月补充账单：租金入账 + 水电费
        Bill extraRent = bill(c2, "RENT", new BigDecimal("1560"), LocalDate.now().minusDays(2));
        extraRent.setStatus("PAID");
        extraRent.setPayTime(LocalDateTime.now().minusDays(1));
        extraRent.setRemark("尾期18天折算租金");
        billRepository.save(extraRent);
        Bill water = bill(c1, "WATER", new BigDecimal("86"), LocalDate.now().minusDays(5));
        water.setStatus("PAID");
        water.setPayTime(LocalDateTime.now().minusDays(4));
        billRepository.save(water);
        Bill electric = bill(c2, "ELECTRIC", new BigDecimal("145"), LocalDate.now().plusDays(5));
        electric.setRemark("电费待缴");
        billRepository.save(electric);

        // 待签约草稿：c3 金融城套四 - 周雨
        Contract c3 = contract("HT20261020003", h4, t4, ld3, agent,
                LocalDate.now().plusDays(5), LocalDate.now().plusMonths(12).plusDays(5), 3, "DRAFT");

        // 已到期历史合同（用于收入报表）：青羊老式套二 - 孙浩
        Contract c4 = contract("HT20250101004", h5, t3, ld2, agent,
                LocalDate.now().minusMonths(15), LocalDate.now().minusMonths(3), 3, "EXPIRED");
        c4.setSignTime(LocalDate.now().minusMonths(15).atTime(9, 0));
        c4.setSignMethod("OFFLINE");
        billsFor(c4, 5);

        // 通知
        Notification n = new Notification();
        n.setTarget(agent.getName());
        n.setTitle("欢迎使用智慧社区数字化租赁服务平台");
        n.setContent("系统已上线用户、房源、租客、合同、费用与统计分析六大模块，合同到期提醒将自动推送。");
        notificationRepository.save(n);
    }

    /** 为合同生成押金+分期租金账单，前 paidPeriods 期租金已缴纳 */
    private void billsFor(Contract c, int paidPeriods) {
        int cycle = c.getPayCycle();
        long totalMonths = ChronoUnit.MONTHS.between(c.getStartDate(), c.getEndDate());
        int periods = (int) Math.max(1, Math.ceil(totalMonths / (double) cycle));
        BigDecimal rentAmount = c.getRent().multiply(BigDecimal.valueOf(cycle));

        Bill deposit = bill(c, "DEPOSIT", c.getDeposit(), c.getStartDate());
        deposit.setStatus("PAID");
        deposit.setPayTime(c.getStartDate().atTime(10, 0));
        billRepository.save(deposit);

        for (int i = 0; i < periods; i++) {
            LocalDate due = c.getStartDate().plusMonths((long) i * cycle);
            Bill rent = bill(c, "RENT", rentAmount, due);
            rent.setRemark(String.format("第%d期租金（%d个月）", i + 1, cycle));
            if (i < paidPeriods) {
                rent.setStatus("PAID");
                rent.setPayTime(due.plusDays(2).atTime(9, 30));
            }
            billRepository.save(rent);
        }
    }

    private Bill bill(Contract c, String type, BigDecimal amount, LocalDate dueDate) {
        Bill b = new Bill();
        b.setContractId(c.getId());
        b.setContractNo(c.getContractNo());
        b.setTenantId(c.getTenantId());
        b.setTenantName(c.getTenantName());
        b.setType(type);
        b.setAmount(amount);
        b.setDueDate(dueDate);
        b.setStatus("UNPAID");
        return b;
    }

    private Contract contract(String no, House h, Tenant t, User landlord, User agent,
                              LocalDate start, LocalDate end, int cycle, String status) {
        Contract c = new Contract();
        c.setContractNo(no);
        c.setHouseId(h.getId());
        c.setHouseAddress(h.getAddress());
        c.setTenantId(t.getId());
        c.setTenantName(t.getName());
        c.setLandlordId(landlord.getId());
        c.setLandlordName(landlord.getName());
        c.setAgentName(agent.getName());
        c.setStartDate(start);
        c.setEndDate(end);
        c.setRent(h.getRent());
        c.setDeposit(h.getDeposit());
        c.setPayCycle(cycle);
        c.setStatus(status);
        return contractRepository.save(c);
    }

    private void viewing(Tenant t, House h, LocalDateTime time, String feedback, int intention) {
        ViewingRecord v = new ViewingRecord();
        v.setTenantId(t.getId());
        v.setTenantName(t.getName());
        v.setHouseId(h.getId());
        v.setHouseTitle(h.getTitle());
        v.setViewTime(time);
        v.setFeedback(feedback);
        v.setIntention(intention);
        viewingRecordRepository.save(v);
    }

    private Tenant tenant(String name, String gender, String idCard, String phone,
                          int age, String occupation, int creditScore, String remark) {
        Tenant t = new Tenant();
        t.setName(name);
        t.setGender(gender);
        t.setIdCard(idCard);
        t.setPhone(phone);
        t.setAge(age);
        t.setOccupation(occupation);
        t.setCreditScore(creditScore);
        t.setRemark(remark);
        return tenantRepository.save(t);
    }

    private House house(String title, String address, String area, String layout, int floor, int totalFloors,
                        int rent, int deposit, String status, User owner, String facilities, String desc, int views) {
        House h = new House();
        h.setTitle(title);
        h.setAddress(address);
        h.setArea(area);
        h.setLayout(layout);
        h.setFloor(floor);
        h.setTotalFloors(totalFloors);
        h.setRent(BigDecimal.valueOf(rent));
        h.setDeposit(BigDecimal.valueOf(deposit));
        h.setStatus(status);
        h.setOwnerId(owner.getId());
        h.setOwnerName(owner.getName());
        h.setFacilities(facilities);
        h.setDescription(desc);
        h.setViewCount(views);
        return houseRepository.save(h);
    }

    private User user(String username, String name, String role, String phone, String email) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("123456");
        u.setName(name);
        u.setRole(role);
        u.setPhone(phone);
        u.setEmail(email);
        u.setStatus(1);
        return userRepository.save(u);
    }
}
