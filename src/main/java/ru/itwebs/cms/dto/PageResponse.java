package ru.itwebs.cms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(name = "PageResponse", description = "Готовый публичный ответ одной страницы. Ключ sections — имя раздела контента; поля группы индексируются по itemKey.")
public record PageResponse(
        @Schema(description = "Ключ страницы, совпадает с последним сегментом URL.", example = "cases")
        String page,
        @Schema(description = "Секции страницы. Каждая секция содержит группы; у одиночного блока groupName равен null.")
        Map<String, List<Group>> sections) {

    @Schema(name = "PageGroup", description = "Одна группа контента: одиночный блок страницы или повторяющаяся карточка.")
    public record Group(
            @Schema(description = "Имя группы карточки; null для одиночного блока секции.", example = "case-1")
            String groupName,
            @Schema(description = "Поля группы, индексированные по itemKey. Значение каждого поля содержит type, value, href и медиа-ссылку.")
            Map<String, Field> fields,
            @Schema(description = "Связанная карточка проекта для записи списка кейсов. Заполнена у cases-page-items и services-page-project-items.")
            Group project) { }

    @Schema(name = "PageField", description = "Редактируемое текстовое, ссылочное или графическое поле контента.")
    public record Field(
            @Schema(description = "ID элемента. Используется для обновления через PUT /api/admin/content/{id}.", example = "123")
            Long id,
            @Schema(description = "Тип поля: TEXT, LINK или IMAGE.", example = "TEXT", allowableValues = {"TEXT", "LINK", "IMAGE"})
            String type,
            @Schema(description = "Метка поля для админского интерфейса.", example = "title")
            String label,
            @Schema(description = "Текст поля или alt-текст изображения.", example = "Проекты которыми мы гордимся")
            String value,
            @Schema(description = "Адрес для LINK-поля; для остальных типов null.", example = "/cases/example")
            String href,
            @Schema(description = "ID связанного файла; null, если изображение ещё не загружено.", example = "45")
            Long mediaId,
            @Schema(description = "Публичный URL файла; null, если изображение ещё не загружено.", example = "/api/media/45")
            String mediaUrl,
            @Schema(description = "Порядок отображения элемента внутри секции.", example = "10")
            int sortOrder) { }
}
