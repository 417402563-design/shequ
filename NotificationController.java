package cn.edu.sdua.shequ.controller;

import cn.edu.sdua.shequ.common.Result;
import cn.edu.sdua.shequ.entity.Notification;
import cn.edu.sdua.shequ.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息通知（账单推送、到期提醒）
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Result<List<Notification>> list(@RequestParam String target) {
        return Result.ok(notificationService.listByTarget(target));
    }

    @GetMapping("/unread")
    public Result<Map<String, Long>> unread(@RequestParam String target) {
        return Result.ok(Map.of("count", notificationService.unreadCount(target)));
    }

    @PutMapping("/read")
    public Result<Void> markRead(@RequestParam String target) {
        notificationService.markAllRead(target);
        return Result.ok();
    }
}
