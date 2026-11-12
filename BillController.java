package cn.edu.sdua.shequ.controller;

import cn.edu.sdua.shequ.common.Result;
import cn.edu.sdua.shequ.entity.Bill;
import cn.edu.sdua.shequ.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 费用管理（租金收取记录、押金处理、账单生成与推送）
 */
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    @GetMapping
    public Result<List<Bill>> list(@RequestParam(required = false) String status,
                                   @RequestParam(required = false) String type,
                                   @RequestParam(required = false) Long tenantId) {
        return Result.ok(billService.list(status, type, tenantId));
    }

    /** 手动新增账单并推送 */
    @PostMapping
    public Result<Bill> create(@RequestBody Bill bill) {
        bill.setId(null);
        return Result.ok(billService.create(bill));
    }

    /** 租金收取：标记已缴费 */
    @PutMapping("/{id}/pay")
    public Result<Bill> pay(@PathVariable Long id) {
        return Result.ok(billService.pay(id));
    }

    /** 押金处理：REFUNDED退还 / DEDUCTED扣除 */
    @PutMapping("/deposit")
    public Result<Bill> handleDeposit(@RequestParam Long contractId,
                                      @RequestParam String handle) {
        return Result.ok(billService.handleDeposit(contractId, handle));
    }

    /** 按合同生成账单 */
    @PostMapping("/generate/{contractId}")
    public Result<Void> generate(@PathVariable Long contractId) {
        billService.generateByContractId(contractId);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        billService.delete(id);
        return Result.ok();
    }
}
