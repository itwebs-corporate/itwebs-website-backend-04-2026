package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import ru.itwebs.cms.dto.AmoCrmExternalCredentialsRequest;
import ru.itwebs.cms.service.AmoCrmIntegrationService;

@RestController
@RequestMapping("/api/integrations/amocrm")
@Tag(name = "amoCRM OAuth", description = "Авторизация подключения amoCRM")
public class AmoCrmIntegrationController {
    private final AmoCrmIntegrationService amoCrm;

    public AmoCrmIntegrationController(AmoCrmIntegrationService amoCrm) { this.amoCrm = amoCrm; }

    @PostMapping(value = "/secrets", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Получить ключи внешней интеграции", description = "Webhook amoCRM передаёт OAuth-реквизиты для временного state.")
    public ResponseEntity<Void> receiveSecrets(@Valid @RequestBody AmoCrmExternalCredentialsRequest request) {
        amoCrm.receiveExternalCredentials(request.client_id(), request.client_secret(), request.state());
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "OAuth callback amoCRM", description = "Обменивает одноразовый OAuth code и сохраняет зашифрованные токены.")
    public String callback(@RequestParam(required = false) String code,
                           @RequestParam(required = false) String state,
                           @RequestParam(required = false) String referer,
                           @RequestParam(required = false) String error) {
        if (error != null && !error.isBlank()) return "Доступ к amoCRM не предоставлен: " + error;
        if (code == null || code.isBlank()) throw new IllegalArgumentException("amoCRM не передала authorization code");
        try {
            amoCrm.completeAuthorization(code, state, referer);
        } catch (Exception exception) {
            throw new IllegalStateException("Не удалось подключить amoCRM: " + exception.getMessage(), exception);
        }
        return "amoCRM подключена. Можно закрыть эту страницу.";
    }
}
