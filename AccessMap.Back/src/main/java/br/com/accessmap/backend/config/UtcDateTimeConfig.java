package br.com.accessmap.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * As datas são guardadas em UTC ({@link br.com.accessmap.backend.BackendApplication} fixa o fuso), mas
 * {@code LocalDateTime} não carrega fuso. Sem o {@code Z}, o app leria "2026-10-19T01:30:00" como horário
 * local do celular e mostraria o dia seguinte para uma avaliação feita às 22:30 em Brasília (#115).
 */
@Configuration
public class UtcDateTimeConfig {

    @Bean
    public SimpleModule utcDateTimeModule() {
        return new SimpleModule("utc-date-time").addSerializer(LocalDateTime.class, new UtcSerializer());
    }

    static class UtcSerializer extends StdSerializer<LocalDateTime> {

        UtcSerializer() {
            super(LocalDateTime.class);
        }

        @Override
        public void serialize(LocalDateTime value, JsonGenerator generator, SerializationContext context) {
            generator.writeString(value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
    }
}
