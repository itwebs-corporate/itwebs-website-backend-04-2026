package ru.itwebs.cms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "amocrm_connection")
public class AmoCrmConnection {
    @Id
    private Long id = 1L;
    @Column(name = "access_token", columnDefinition = "text")
    private String accessToken;
    @Column(name = "refresh_token", columnDefinition = "text")
    private String refreshToken;
    @Column(name = "client_id", columnDefinition = "text")
    private String clientId;
    @Column(name = "client_secret", columnDefinition = "text")
    private String clientSecret;
    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;
    @Column(name = "pending_state", length = 64)
    private String pendingState;
    @Column(name = "pending_state_at")
    private Instant pendingStateAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public Instant getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(Instant tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }
    public String getPendingState() { return pendingState; }
    public void setPendingState(String pendingState) { this.pendingState = pendingState; }
    public Instant getPendingStateAt() { return pendingStateAt; }
    public void setPendingStateAt(Instant pendingStateAt) { this.pendingStateAt = pendingStateAt; }
}
