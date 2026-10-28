package org.yuktisetu.core.time;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Pins the whole platform to IST, so nothing has to be converted to be read.
 *
 * Three places used to disagree. The JVM took whatever zone the host machine had;
 * Hibernate was told to talk to the database in UTC, so the TIMESTAMP (no zone)
 * columns held UTC wall-clock time -- `users.created_at` read 08:44 for something
 * that happened at 14:14 -- while the TIMESTAMPTZ columns beside them showed IST;
 * and the API wrote Instants as UTC ("...T18:06:04Z"). Each is fixed here or in the
 * service properties (hibernate.jdbc.time_zone=Asia/Kolkata).
 *
 * The static block runs when this class is loaded during component scanning, which
 * is before the datasource and the JSON mapper are created.
 */
@Slf4j
@Configuration
public class IstTimeConfiguration {

    static {
        TimeZone.setDefault(IstTime.TIME_ZONE);
    }

    /**
     * JSON in IST. java.util.Date follows the mapper's default zone; Instant is
     * always written in UTC by Jackson regardless of it, so it gets its own
     * serialiser: 2026-10-19T23:36:04+05:30 rather than 2026-10-19T18:06:04Z. Both
     * are the same instant, so clients that parse them (new Date(...)) are
     * unaffected; people reading the raw response no longer have to add 5:30.
     */
    @Bean
    public JsonMapperBuilderCustomizer istJsonMapperCustomizer() {
        return builder -> {
            builder.defaultTimeZone(IstTime.TIME_ZONE);
            SimpleModule module = new SimpleModule("ist-instant");
            module.addSerializer(Instant.class, new IstInstantSerializer());
            builder.addModule(module);
        };
    }

    static final class IstInstantSerializer extends ValueSerializer<Instant> {
        private static final DateTimeFormatter FORMAT =
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(IstTime.ZONE);

        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(FORMAT.format(value));
        }
    }
}
