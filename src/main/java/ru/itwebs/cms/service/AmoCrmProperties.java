package ru.itwebs.cms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AmoCrmProperties {
    @Value("${app.amocrm.account-url:https://itwebs.amocrm.ru}")
    private String accountUrl;
    @Value("${app.amocrm.redirect-uri:https://itwebs.ru/api/integrations/amocrm/callback}")
    private String redirectUri;
    @Value("${app.amocrm.pipeline-name:Заявки с сайта}")
    private String pipelineName;
    @Value("${app.amocrm.status-name:Новая заявка}")
    private String statusName;
    @Value("${app.amocrm.token-encryption-key:}")
    private String tokenEncryptionKey;
    @Value("${app.amocrm.responsible-user-id:}")
    private String responsibleUserId;

    public String getAccountUrl() { return trimSlash(accountUrl); }
    public String getRedirectUri() { return redirectUri; }
    public String getPipelineName() { return pipelineName; }
    public String getStatusName() { return statusName; }
    public String getTokenEncryptionKey() { return tokenEncryptionKey; }
    public String getResponsibleUserId() { return responsibleUserId; }

    public boolean credentialsConfigured() {
        return present(redirectUri) && present(tokenEncryptionKey);
    }

    private static boolean present(String value) { return value != null && !value.isBlank(); }
    private static String trimSlash(String value) {
        if (value == null) return "";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
