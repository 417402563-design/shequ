package cn.edu.sdua.shequ.service;

import cn.edu.sdua.shequ.entity.Notification;
import cn.edu.sdua.shequ.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 消息通知服务（账单推送/到期提醒的落地通道）
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void push(String target, String title, String content) {
        if (target == null || target.isBlank()) return;
        Notification n = new Notification();
        n.setTarget(target);
        n.setTitle(title);
        n.setContent(content);
        notificationRepository.save(n);
    }

    public List<Notification> listByTarget(String target) {
        return notificationRepository.findTop20ByTargetOrderByIdDesc(target);
    }

    public long unreadCount(String target) {
        return notificationRepository.findTop20ByTargetOrderByIdDesc(target).stream()
                .filter(n -> n.getReadFlag() == 0).count();
    }

    @Transactional
    public void markAllRead(String target) {
        notificationRepository.markAllRead(target);
    }
}
