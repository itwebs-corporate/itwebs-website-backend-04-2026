package ru.itwebs.cms.dto;

import ru.itwebs.cms.entity.Lead;
import java.time.Instant;

public record LeadResponse(Long id, String name, String phone, String email, String message,
                           String contactMethod, Instant createdAt, boolean processed) {
    public static LeadResponse from(Lead lead) {
        return new LeadResponse(lead.getId(), lead.getName(), lead.getPhone(), lead.getEmail(),
                lead.getMessage(), lead.getContactMethod(), lead.getCreatedAt(), lead.isProcessed());
    }
}
