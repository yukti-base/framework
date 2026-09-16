package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.NotificationTemplate;
import org.yuktisetu.model.NotificationChannel;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByTemplateKeyAndChannelAndLocaleAndIsActiveTrueAndIsDeletedFalse(
            String templateKey, NotificationChannel channel, String locale);

    List<NotificationTemplate> findByIsActiveTrueAndIsDeletedFalse();
}
