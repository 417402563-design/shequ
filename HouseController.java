package cn.edu.sdua.shequ.controller;

import cn.edu.sdua.shequ.common.Result;
import cn.edu.sdua.shequ.entity.House;
import cn.edu.sdua.shequ.service.HouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 房源管理（录入/编辑/下架/状态更新）
 */
@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
public class HouseController {

    private final HouseService houseService;

    @GetMapping
    public Result<List<House>> list(@RequestParam(required = false) String status,
                                    @RequestParam(required = false) String area,
                                    @RequestParam(required = false) String keyword) {
        return Result.ok(houseService.list(status, area, keyword));
    }

    @GetMapping("/areas")
    public Result<List<String>> areas() {
        return Result.ok(houseService.areas());
    }

    @GetMapping("/{id}")
    public Result<House> detail(@PathVariable Long id) {
        return Result.ok(houseService.detail(id));
    }

    @PostMapping
    public Result<House> create(@RequestBody House house) {
        house.setId(null);
        return Result.ok(houseService.create(house));
    }

    @PutMapping("/{id}")
    public Result<House> update(@PathVariable Long id, @RequestBody House house) {
        return Result.ok(houseService.update(id, house));
    }

    /** 下架 */
    @PutMapping("/{id}/off")
    public Result<House> offShelf(@PathVariable Long id) {
        return Result.ok(houseService.offShelf(id));
    }

    /** 状态更新 */
    @PutMapping("/{id}/status")
    public Result<House> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return Result.ok(houseService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        houseService.delete(id);
        return Result.ok();
    }
}
