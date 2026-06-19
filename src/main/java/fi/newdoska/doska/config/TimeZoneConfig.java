package fi.newdoska.doska.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {

    public static final ZoneId FINLAND = ZoneId.of("Europe/Helsinki");

    @Value("${app.timezone:Europe/Helsinki}")
    private String timezoneId;

    @PostConstruct
    void setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(timezoneId));
    }

    @Bean
    Jackson2ObjectMapperBuilderCustomizer finlandJacksonTimeZone() {
        return builder -> builder.timeZone(TimeZone.getTimeZone(timezoneId));
    }
}
