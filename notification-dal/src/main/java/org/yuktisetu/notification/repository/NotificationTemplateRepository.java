package org.yuktisetu.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.notification.db.NotificationTemplate;
import org.yuktisetu.notification.model.NotificationChannel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByTemplateKeyAndChannelAndLocaleAndIsActiveTrueAndIsDeletedFalse(
            String templateKey, NotificationChannel channel, String locale);

    /**
     * Every template one dispatch needs, in a single query.
     *
     * The per-channel finder above returns one row, which would mean one query
     * per channel per dispatch -- and a dispatch that goes out on in-app, email
     * and a Google Group would pay three round trips before it has rendered
     * anything. Fetching the whole set once and indexing it by channel in memory
     * keeps ingestion's query count fixed no matter how many channels or
     * recipients a blast carries.
     */
    List<NotificationTemplate> findByTemplateKeyAndLocaleAndChannelInAndIsActiveTrueAndIsDeletedFalse(
            String templateKey, String locale, Collection<NotificationChannel> channels);

    List<NotificationTemplate> findByIsActiveTrueAndIsDeletedFalse();
}
