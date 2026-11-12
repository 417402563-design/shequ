package cn.edu.sdua.shequ.controller;

import cn.edu.sdua.shequ.common.Result;
import cn.edu.sdua.shequ.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 统计分析（出租率统计、收入报表、热门区域分析）
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(statsService.overview());
    }

    @GetMapping("/monthly-income")
    public Result<List<Map<String, Object>>> monthlyIncome() {
        return Result.ok(statsService.monthlyIncome());
    }

    @GetMapping("/house-status")
    public Result<List<Map<String, Object>>> houseStatus() {
        return Result.ok(statsService.houseStatusStats());
    }

    @GetMapping("/area")
    public Result<List<Map<String, Object>>> areaStats() {
        return Result.ok(statsService.areaStats());
    }
}
