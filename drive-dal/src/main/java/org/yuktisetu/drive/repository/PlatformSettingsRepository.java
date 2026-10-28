package org.yuktisetu.drive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.PlatformSettings;

@Repository
public interface PlatformSettingsRepository extends JpaRepository<PlatformSettings, Short> {

    /** The one row. Missing means the migration was not run, which reads as "off". */
    default PlatformSettings current() {
        return findById(PlatformSettings.SINGLETON_ID).orElseGet(PlatformSettings::new);
    }
}
