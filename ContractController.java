package cn.edu.sdua.shequ.controller;

import cn.edu.sdua.shequ.common.Result;
import cn.edu.sdua.shequ.entity.Contract;
import cn.edu.sdua.shequ.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 合同管理（模板生成、电子签约、到期提醒）
 */
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @GetMapping
    public Result<List<Contract>> list(@RequestParam(required = false) String status,
                                       @RequestParam(required = false) String keyword) {
        return Result.ok(contractService.list(status, keyword));
    }

    /** 到期提醒：查询N天内即将到期的生效合同 */
    @GetMapping("/expiring")
    public Result<List<Contract>> expiring(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(contractService.expiringWithin(days));
    }

    /** 合同模板生成（创建待签合同） */
    @PostMapping
    public Result<Contract> create(@RequestBody Contract contract) {
        contract.setId(null);
        return Result.ok(contractService.createDraft(contract));
    }

    /** 预览合同模板全文 */
    @GetMapping("/{id}/content")
    public Result<Map<String, String>> content(@PathVariable Long id) {
        return Result.ok(Map.of("content", contractService.renderTemplate(id)));
    }

    /** 电子签约 */
    @PutMapping("/{id}/sign")
    public Result<Contract> sign(@PathVariable Long id,
                                 @RequestParam(defaultValue = "ONLINE") String signMethod) {
        return Result.ok(contractService.sign(id, signMethod));
    }

    /** 提前退租（终止合同并处理押金） */
    @PutMapping("/{id}/terminate")
    public Result<Contract> terminate(@PathVariable Long id,
                                      @RequestParam(defaultValue = "REFUNDED") String depositHandle) {
        return Result.ok(contractService.terminate(id, depositHandle));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return Result.ok();
    }
}
