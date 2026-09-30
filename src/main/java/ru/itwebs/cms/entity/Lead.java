package ru.itwebs.cms.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "leads")
public class Lead {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(length = 80) private String phone;
    @Column(length = 255) private String email;
    @Column(nullable = false, columnDefinition = "text") private String message;
    @Column(length = 30) private String contactMethod;
    @Column(nullable = false) private Instant createdAt = Instant.now();
    private boolean processed;
    @Column(name = "amocrm_lead_id") private Long amoCrmLeadId;
    @Column(name = "amocrm_note_synced") private boolean amoCrmNoteSynced;
    @Column(name = "amocrm_sync_status", length = 32) private String amoCrmSyncStatus;
    @Column(name = "amocrm_sync_error", length = 1000) private String amoCrmSyncError;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getContactMethod() { return contactMethod; }
    public void setContactMethod(String contactMethod) { this.contactMethod = contactMethod; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }
    public Long getAmoCrmLeadId() { return amoCrmLeadId; }
    public void setAmoCrmLeadId(Long amoCrmLeadId) { this.amoCrmLeadId = amoCrmLeadId; }
    public boolean isAmoCrmNoteSynced() { return amoCrmNoteSynced; }
    public void setAmoCrmNoteSynced(boolean amoCrmNoteSynced) { this.amoCrmNoteSynced = amoCrmNoteSynced; }
    public String getAmoCrmSyncStatus() { return amoCrmSyncStatus; }
    public void setAmoCrmSyncStatus(String amoCrmSyncStatus) { this.amoCrmSyncStatus = amoCrmSyncStatus; }
    public String getAmoCrmSyncError() { return amoCrmSyncError; }
    public void setAmoCrmSyncError(String amoCrmSyncError) { this.amoCrmSyncError = amoCrmSyncError; }
}
