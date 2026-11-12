package cn.edu.sdua.shequ.controller;

import cn.edu.sdua.shequ.common.Result;
import cn.edu.sdua.shequ.entity.Tenant;
import cn.edu.sdua.shequ.entity.ViewingRecord;
import cn.edu.sdua.shequ.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租客管理（信息登记、信用评估、看房记录）
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping("/tenants")
    public Result<List<Tenant>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(tenantService.list(keyword));
    }

    @PostMapping("/tenants")
    public Result<Tenant> create(@RequestBody Tenant tenant) {
        tenant.setId(null);
        return Result.ok(tenantService.save(tenant));
    }

    @PutMapping("/tenants/{id}")
    public Result<Tenant> update(@PathVariable Long id, @RequestBody Tenant tenant) {
        tenant.setId(id);
        return Result.ok(tenantService.save(tenant));
    }

    @DeleteMapping("/tenants/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tenantService.delete(id);
        return Result.ok();
    }

    @GetMapping("/tenants/{id}/viewings")
    public Result<List<ViewingRecord>> viewings(@PathVariable Long id) {
        return Result.ok(tenantService.viewings(id));
    }

    @PostMapping("/viewings")
    public Result<ViewingRecord> addViewing(@RequestBody ViewingRecord record) {
        record.setId(null);
        return Result.ok(tenantService.addViewing(record));
    }

    @DeleteMapping("/viewings/{id}")
    public Result<Void> deleteViewing(@PathVariable Long id) {
        tenantService.deleteViewing(id);
        return Result.ok();
    }
}
