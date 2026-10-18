package ru.itwebs.cms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class StructuredPayloadLimits {
    private final ObjectMapper mapper;
    public StructuredPayloadLimits(ObjectMapper mapper) { this.mapper = mapper; }
    public void validate(Object payload) {
        try {
            if (mapper.writeValueAsBytes(payload).length > 1_000_000)
                throw new IllegalArgumentException("Содержимое записи превышает 1 МБ");
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("Некорректное содержимое записи", error);
        }
    }
}
