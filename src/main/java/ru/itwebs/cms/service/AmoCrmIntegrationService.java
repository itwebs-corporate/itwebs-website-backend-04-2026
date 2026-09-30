package ru.itwebs.cms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.itwebs.cms.entity.AmoCrmConnection;
import ru.itwebs.cms.entity.Lead;
import ru.itwebs.cms.repository.AmoCrmConnectionRepository;
import ru.itwebs.cms.repository.LeadRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AmoCrmIntegrationService {
    private static final Logger log = LoggerFactory.getLogger(AmoCrmIntegrationService.class);
    private final AmoCrmProperties properties;
    private final AmoCrmConnectionRepository connections;
    private final LeadRepository leads;
    private final AmoCrmTokenCipher cipher;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public AmoCrmIntegrationService(AmoCrmProperties properties, AmoCrmConnectionRepository connections,
                                    LeadRepository leads, AmoCrmTokenCipher cipher, ObjectMapper mapper) {
        this.properties = properties;
        this.connections = connections;
        this.leads = leads;
        this.cipher = cipher;
        this.mapper = mapper;
    }

    public boolean isReady() {
        if (!properties.credentialsConfigured()) return false;
        return connections.findById(1L).map(c -> c.getRefreshToken() != null && !c.getRefreshToken().isBlank()).orElse(false);
    }

    public synchronized String createAuthorizationPage() {
        requireConfigured();
        AmoCrmConnection connection = connection();
        String state = UUID.randomUUID().toString();
        connection.setPendingState(state);
        connection.setPendingStateAt(Instant.now());
        connections.save(connection);
        String redirect = html(properties.getRedirectUri());
        String secrets = html("https://itwebs.ru/api/integrations/amocrm/secrets");
        return "<!doctype html><html lang=\"ru\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<title>Подключение amoCRM</title><body style=\"font:16px sans-serif;padding:3rem;text-align:center\">" +
                "<h1>Подключение amoCRM</h1><p>Нажмите кнопку и разрешите доступ к аккаунту ITWEBS.</p>" +
                "<script class=\"amocrm_oauth\" charset=\"utf-8\" data-name=\"ITWEBS Website Leads\" " +
                "data-description=\"Передача заявок с сайта ITWEBS в amoCRM\" data-redirect_uri=\"" + redirect +
                "\" data-secrets_uri=\"" + secrets + "\" data-scopes=\"crm\" data-title=\"Подключить amoCRM\" " +
                "data-state=\"" + state + "\" data-mode=\"popup\" src=\"https://www.amocrm.ru/auth/button.min.js\"></script>" +
                "</body></html>";
    }

    public synchronized void completeAuthorization(String code, String state, String referer) throws Exception {
        requireConfigured();
        AmoCrmConnection connection = connections.findById(1L)
                .orElseThrow(() -> new IllegalArgumentException("Сначала откройте страницу подключения amoCRM"));
        validatePendingState(connection, state);
        if (connection.getClientId() == null || connection.getClientSecret() == null)
            throw new IllegalArgumentException("amoCRM ещё не передала данные внешней интеграции");
        validateAccountReferer(referer);
        JsonNode tokens = exchangeToken(connection, "authorization_code", "code", code);
        saveTokens(connection, tokens);
        connection.setPendingState(null);
        connection.setPendingStateAt(null);
        connection.setClientId(null);
        connection.setClientSecret(null);
        connections.save(connection);
        log.info("amoCRM OAuth connected for configured account");
    }

    public synchronized void receiveExternalCredentials(String clientId, String clientSecret, String state) {
        requireConfigured();
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank())
            throw new IllegalArgumentException("amoCRM передала пустые реквизиты внешней интеграции");
        AmoCrmConnection connection = connections.findById(1L)
                .orElseThrow(() -> new IllegalArgumentException("Сначала откройте страницу подключения amoCRM"));
        validatePendingState(connection, state);
        if (connection.getClientId() != null && connection.getClientSecret() != null &&
                clientId.equals(cipher.decrypt(connection.getClientId())) &&
                clientSecret.equals(cipher.decrypt(connection.getClientSecret()))) {
            return;
        }
        connection.setClientId(cipher.encrypt(clientId));
        connection.setClientSecret(cipher.encrypt(clientSecret));
        connections.save(connection);
    }

    @Async
    public void sendLead(Long leadId) {
        Lead lead = leads.findById(leadId).orElseThrow();
        if (!isReady()) {
            lead.setAmoCrmSyncStatus("WAITING_FOR_AUTH");
            lead.setAmoCrmSyncError(null);
            leads.save(lead);
            return;
        }
        synchronizeLead(leadId);
    }

    @Scheduled(fixedDelayString = "${app.amocrm.retry-delay-ms:60000}")
    public void retryFailedLeads() {
        if (!isReady()) return;
        for (Lead lead : leads.findTop50ByAmoCrmSyncStatusInOrderByCreatedAtAsc(List.of("PENDING", "ERROR"))) {
            synchronizeLead(lead.getId());
        }
    }

    private void synchronizeLead(Long leadId) {
        Lead lead = leads.findById(leadId).orElse(null);
        if (lead == null) return;
        try {
            if (lead.getAmoCrmLeadId() == null) {
                long[] target = findTargetStage();
                ArrayNode request = mapper.createArrayNode();
                ObjectNode deal = request.addObject();
                deal.put("name", "Заявка с сайта — " + lead.getName());
                deal.put("pipeline_id", target[0]);
                deal.put("status_id", target[1]);
                deal.put("request_id", "itwebs-lead-" + lead.getId());
                ObjectNode contact = mapper.createObjectNode().put("first_name", lead.getName());
                ArrayNode fields = contact.putArray("custom_fields_values");
                if (lead.getPhone() != null && !lead.getPhone().isBlank()) {
                    fields.addObject().put("field_code", "PHONE").putArray("values")
                            .addObject().put("enum_code", "WORK").put("value", lead.getPhone());
                }
                if (lead.getEmail() != null && !lead.getEmail().isBlank()) {
                    fields.addObject().put("field_code", "EMAIL").putArray("values")
                            .addObject().put("enum_code", "WORK").put("value", lead.getEmail());
                }
                deal.putObject("_embedded").putArray("contacts").add(contact);
                String responsibleId = properties.getResponsibleUserId();
                if (responsibleId != null && !responsibleId.isBlank()) {
                    deal.put("responsible_user_id", Long.parseLong(responsibleId));
                    contact.put("responsible_user_id", Long.parseLong(responsibleId));
                }
                JsonNode created = api("POST", "/api/v4/leads/complex", request);
                JsonNode result = created.isArray() ? created.path(0) : created.path("_embedded").path("leads").path(0);
                if (result.path("id").isMissingNode() || result.path("id").isNull())
                    throw new IllegalStateException("amoCRM не вернула ID созданной сделки");
                lead.setAmoCrmLeadId(result.path("id").asLong());
                lead.setAmoCrmSyncStatus("PENDING_NOTE");
                lead.setAmoCrmSyncError(null);
                lead = leads.save(lead);
            }

            if (!lead.isAmoCrmNoteSynced()) {
                ArrayNode notes = mapper.createArrayNode();
                ObjectNode note = notes.addObject().put("note_type", "common");
                ObjectNode params = note.putObject("params");
                params.put("text", "Заявка с сайта\nИмя: " + lead.getName() +
                        "\nТелефон: " + valueOrDash(lead.getPhone()) +
                        "\nПочта: " + valueOrDash(lead.getEmail()) +
                        "\nПредпочтительный способ связи: " + valueOrDash(lead.getContactMethod()) +
                        "\n\nСообщение:\n" + lead.getMessage());
                api("POST", "/api/v4/leads/" + lead.getAmoCrmLeadId() + "/notes", notes);
                lead.setAmoCrmNoteSynced(true);
            }
            lead.setAmoCrmSyncStatus("SYNCED");
            lead.setAmoCrmSyncError(null);
            leads.save(lead);
        } catch (Exception exception) {
            log.warn("Could not sync website lead {} to amoCRM: {}", leadId, safeMessage(exception));
            lead.setAmoCrmSyncStatus("ERROR");
            lead.setAmoCrmSyncError(safeMessage(exception));
            leads.save(lead);
        }
    }

    private long[] findTargetStage() throws Exception {
        JsonNode pipelines = api("GET", "/api/v4/leads/pipelines?limit=250", null)
                .path("_embedded").path("pipelines");
        for (JsonNode pipeline : pipelines) {
            if (!same(pipeline.path("name").asText(), properties.getPipelineName())) continue;
            JsonNode statuses = pipeline.path("_embedded").path("statuses");
            for (JsonNode status : statuses) {
                if (same(status.path("name").asText(), properties.getStatusName()))
                    return new long[]{pipeline.path("id").asLong(), status.path("id").asLong()};
            }
            throw new IllegalStateException("Этап amoCRM не найден в воронке «" + properties.getPipelineName() + "»: «" + properties.getStatusName() + "»");
        }
        throw new IllegalStateException("Воронка amoCRM не найдена: «" + properties.getPipelineName() + "»");
    }

    private synchronized String accessToken() throws Exception {
        AmoCrmConnection connection = connections.findById(1L)
                .orElseThrow(() -> new IllegalStateException("amoCRM ещё не авторизована"));
        if (connection.getAccessToken() != null && connection.getTokenExpiresAt() != null &&
                connection.getTokenExpiresAt().isAfter(Instant.now().plusSeconds(90)))
            return cipher.decrypt(connection.getAccessToken());
        if (connection.getRefreshToken() == null) throw new IllegalStateException("amoCRM ещё не авторизована");
        JsonNode tokens = exchangeToken(connection, "refresh_token", "refresh_token", cipher.decrypt(connection.getRefreshToken()));
        saveTokens(connection, tokens);
        connections.save(connection);
        return cipher.decrypt(connection.getAccessToken());
    }

    private JsonNode api(String method, String path, JsonNode body) throws Exception {
        String token = accessToken();
        HttpResponse<String> response = apiRequest(method, path, body, token);
        if (response.statusCode() == 401) {
            synchronized (this) {
                AmoCrmConnection connection = connections.findById(1L).orElseThrow();
                JsonNode refreshed = exchangeToken(connection, "refresh_token", "refresh_token", cipher.decrypt(connection.getRefreshToken()));
                saveTokens(connection, refreshed);
                connections.save(connection);
                token = cipher.decrypt(connection.getAccessToken());
            }
            response = apiRequest(method, path, body, token);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IllegalStateException("amoCRM API вернула HTTP " + response.statusCode());
        if (response.body() == null || response.body().isBlank()) return mapper.createObjectNode();
        return mapper.readTree(response.body());
    }

    private HttpResponse<String> apiRequest(String method, String path, JsonNode body, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(properties.getAccountUrl() + path))
                .timeout(Duration.ofSeconds(25)).header("Authorization", "Bearer " + token)
                .header("Accept", "application/json");
        if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
        else builder.header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode exchangeToken(AmoCrmConnection connection, String grantType, String tokenParameter, String tokenValue) throws Exception {
        ObjectNode payload = mapper.createObjectNode().put("client_id", cipher.decrypt(connection.getClientId()))
                .put("client_secret", cipher.decrypt(connection.getClientSecret())).put("grant_type", grantType)
                .put(tokenParameter, tokenValue).put("redirect_uri", properties.getRedirectUri());
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getAccountUrl() + "/oauth2/access_token"))
                .timeout(Duration.ofSeconds(25)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload))).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IllegalStateException("amoCRM OAuth вернул HTTP " + response.statusCode());
        JsonNode tokens = mapper.readTree(response.body());
        if (tokens.path("access_token").asText().isBlank() || tokens.path("refresh_token").asText().isBlank())
            throw new IllegalStateException("amoCRM OAuth не вернул токены");
        return tokens;
    }

    private void saveTokens(AmoCrmConnection connection, JsonNode tokens) {
        connection.setAccessToken(cipher.encrypt(tokens.path("access_token").asText()));
        connection.setRefreshToken(cipher.encrypt(tokens.path("refresh_token").asText()));
        connection.setTokenExpiresAt(Instant.now().plusSeconds(tokens.path("expires_in").asLong(86400)));
    }

    private void validateAccountReferer(String referer) {
        if (referer == null || referer.isBlank()) return;
        String normalized = referer.contains("://") ? referer : "https://" + referer;
        try {
            String receivedHost = URI.create(normalized).getHost();
            String expectedHost = URI.create(properties.getAccountUrl()).getHost();
            if (receivedHost != null && expectedHost != null && !receivedHost.equalsIgnoreCase(expectedHost))
                throw new IllegalArgumentException("OAuth callback пришёл не из настроенного аккаунта amoCRM");
        } catch (IllegalArgumentException exception) {
            throw exception;
        }
    }

    private AmoCrmConnection connection() {
        return connections.findById(1L).orElseGet(() -> {
            AmoCrmConnection value = new AmoCrmConnection();
            return connections.save(value);
        });
    }

    private void requireConfigured() {
        if (!properties.credentialsConfigured())
            throw new IllegalStateException("Настройте AMOCRM_TOKEN_ENCRYPTION_KEY на сервере");
    }

    private void validatePendingState(AmoCrmConnection connection, String state) {
        if (connection.getPendingState() == null || !connection.getPendingState().equals(state) ||
                connection.getPendingStateAt() == null || connection.getPendingStateAt().isBefore(Instant.now().minus(Duration.ofMinutes(15))))
            throw new IllegalArgumentException("Ссылка авторизации устарела или state не совпадает; откройте страницу подключения заново");
    }

    private static String html(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
    private static boolean same(String first, String second) { return first.trim().equalsIgnoreCase(second.trim()); }
    private static String valueOrDash(String value) { return value == null || value.isBlank() ? "—" : value; }
    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) message = exception.getClass().getSimpleName();
        return message.length() > 950 ? message.substring(0, 950) : message;
    }
}
