package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itwebs.cms.dto.PageResponse;
import ru.itwebs.cms.service.PageService;

@RestController
@RequestMapping("/api/pages")
@Tag(name = "Публичные страницы", description = "Собранные данные страниц для прямого использования фронтендом. В ответ включаются только опубликованные записи; общие шапка, форма и подвал доступны в sections каждого ответа.")
public class PublicPageController {
    private final PageService pages;

    public PublicPageController(PageService pages) {
        this.pages = pages;
    }

    @GetMapping("/services")
    @Operation(
            summary = "Получить страницу услуг",
            description = "Возвращает готовый контент страницы услуг одним ответом: секции, каталог, этапы, задачи, модели, общие блоки и проекты. Карточки услуг и контент сгруппированы по groupName; поля внутри fields доступны по itemKey. У каждой записи fields есть id для обновления через админский CRUD. Изображения возвращают mediaUrl.",
            responses = @ApiResponse(responseCode = "200", description = "Страница услуг",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class),
                            examples = @ExampleObject(name = "Услуги", value = """
                                    {
                                      "page": "services",
                                      "sections": {
                                        "services-page": [
                                          {
                                            "groupName": null,
                                            "fields": {
                                              "title": {"id": 12, "type": "TEXT", "label": "title", "value": "Услуги ITWEBS — полный цикл разработки", "href": null, "mediaId": null, "mediaUrl": null, "sortOrder": 12}
                                            },
                                            "project": null
                                          }
                                        ],
                                        "services-page-catalog": [
                                          {
                                            "groupName": "web-service-1",
                                            "fields": {
                                              "title": {"id": 20, "type": "TEXT", "label": "title", "value": "Корпоративный сайт под ключ", "href": null, "mediaId": null, "mediaUrl": null, "sortOrder": 20},
                                              "price": {"id": 21, "type": "TEXT", "label": "price", "value": "от 350.000 ₽", "href": null, "mediaId": null, "mediaUrl": null, "sortOrder": 21}
                                            },
                                            "project": null
                                          }
                                        ],
                                        "services-page-project-items": [
                                          {
                                            "groupName": "project-1",
                                            "fields": {"projectGroup": {"id": 90, "type": "TEXT", "label": "projectGroup", "value": "project-1", "href": null, "mediaId": null, "mediaUrl": null, "sortOrder": 90}},
                                            "project": {"groupName": "project-1", "fields": {"title": {"id": 91, "type": "TEXT", "label": "title", "value": "Bioflora — платформа микробиомной диагностики кожи", "href": null, "mediaId": null, "mediaUrl": null, "sortOrder": 91}}, "project": null}
                                          }
                                        ]
                                      }
                                    }
                                    """))))
    public PageResponse services() {
        return pages.services();
    }

    @GetMapping("/cases")
    @Operation(
            summary = "Получить страницу кейсов",
            description = "Возвращает готовый контент страницы кейсов одним ответом. Фильтры перечислены в cases-page-filters. Каждая группа cases-page-items содержит связанные фильтры (filter1, filter2, ...), а поле project — полную карточку проекта с title, category, description, details и image. Кейс может иметь несколько фильтров; для фильтрации сравнивайте значения filterN с полем key фильтра. Повторяющиеся визуальные копии одной карточки возвращаются как одна запись.",
            responses = @ApiResponse(responseCode = "200", description = "Страница кейсов",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class),
                            examples = @ExampleObject(name = "Кейсы и фильтры", value = """
                                    {
                                      "page": "cases",
                                      "sections": {
                                        "cases-page-filters": [
                                          {"groupName": "filter-mobile", "fields": {"key": {"id": 30, "type": "TEXT", "label": "key", "value": "mobile", "href": null, "mediaId": null, "mediaUrl": null, "sortOrder": 30}, "label": {"id": 31, "type": "TEXT", "label": "label", "value": "Mobile", "href": null, "mediaId": null, "mediaUrl": null, "sortOrder": 31}}, "project": null},
                                          {"groupName": "filter-ai", "fields": {"key": {"id": 32, "type": "TEXT", "label": "key", "value": "ai", "href": null, "mediaId": null, "mediaUrl": null, "sortOrder": 32}, "label": {"id": 33, "type": "TEXT", "label": "label", "value": "AI", "href": null, "mediaId": null, "mediaUrl": null, "sortOrder": 33}}, "project": null}
                                        ],
                                        "cases-page-items": [
                                          {
                                            "groupName": "case-1",
                                            "fields": {
                                              "projectGroup": {"id": 40, "type": "TEXT", "label": "projectGroup", "value": "project-1", "href": null, "mediaId": null, "mediaUrl": null, "sortOrder": 40},
                                              "filter1": {"id": 41, "type": "TEXT", "label": "filter1", "value": "web", "href": null, "mediaId": null, "mediaUrl": null, "sortOrder": 41},
                                              "filter2": {"id": 42, "type": "TEXT", "label": "filter2", "value": "ai", "href": null, "mediaId": null, "mediaUrl": null, "sortOrder": 42}
                                            },
                                            "project": {
                                              "groupName": "project-1",
                                              "fields": {
                                                "title": {"id": 50, "type": "TEXT", "label": "title", "value": "Bioflora — платформа микробиомной диагностики кожи", "href": null, "mediaId": null, "mediaUrl": null, "sortOrder": 50},
                                                "category": {"id": 51, "type": "TEXT", "label": "category", "value": "Web разработка", "href": null, "mediaId": null, "mediaUrl": null, "sortOrder": 51},
                                                "details": {"id": 52, "type": "LINK", "label": "details", "value": "Подробнее", "href": "/cases/skin-insights", "mediaId": null, "mediaUrl": null, "sortOrder": 52},
                                                "image": {"id": 53, "type": "IMAGE", "label": "image", "value": "Bioflora — платформа микробиомной диагностики кожи", "href": null, "mediaId": 7, "mediaUrl": "/api/media/7", "sortOrder": 53}
                                              },
                                              "project": null
                                            }
                                          }
                                        ]
                                      }
                                    }
                                    """))))
    public PageResponse cases() {
        return pages.cases();
    }
}
