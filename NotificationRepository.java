package cn.edu.sdua.shequ.repository;

import cn.edu.sdua.shequ.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop20ByTargetOrderByIdDesc(String target);

    @Modifying
    @Query("update Notification n set n.readFlag = 1 where n.target = ?1")
    void markAllRead(String target);
}
