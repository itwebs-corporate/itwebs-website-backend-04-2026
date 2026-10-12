package ru.itwebs.cms.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import ru.itwebs.cms.entity.ContentItem;
import ru.itwebs.cms.repository.ContentRepository;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class OutstaffPageContent {
    @Bean
    @Order(4)
    CommandLineRunner seedOutstaffPage(ContentRepository repository) {
        return args -> {
            if (repository.existsBySectionNameAndItemKeyAndGroupName("_system", "outstaffPageSeedV1", null)) return;
            Writer w = new Writer(repository);
            w.add("outstaff-page", null, "eyebrow", "TEXT", "Надзаголовок", "# Код, который работает", null);
            w.add("outstaff-page", null, "title", "TEXT", "Заголовок", "Предоставление разработчиков (аутстаффинг)", null);
            w.add("outstaff-page", null, "subtitle", "TEXT", "Подзаголовок", "Подключаем ИТ-специалистов к проекту в сжатые сроки", null);
            w.add("outstaff-page", null, "description", "TEXT", "Описание", "Предоставляем разработчиков, тестировщиков, аналитиков и инженеров по сопровождению инфраструктуры для усиления вашей команды.", null);
            w.add("outstaff-page", null, "specialists", "LINK", "specialists", "Посмотреть доступных разработчиков", "/outstaff#specialists");
            w.add("outstaff-page", null, "cta", "LINK", "cta", "Обсудить услугу", "/outstaff#lead-form");
            w.add("outstaff-page-use-cases", null, "eyebrow", "TEXT", "Надзаголовок", "Когда это подходит", null);
            w.add("outstaff-page-use-cases", null, "title", "TEXT", "Заголовок", "В каких случаях вам подходит ИТ-аутстаффинг", null);
            w.add("outstaff-page-use-case-items", "item-1", "tag", "TEXT", "Карточка 1: tag", "# Срочность", null);
            w.add("outstaff-page-use-case-items", "item-1", "title", "TEXT", "Карточка 1: title", "Нужно срочно усилить команду", null);
            w.add("outstaff-page-use-case-items", "item-1", "description", "TEXT", "Карточка 1: description", "Предоставим опытных разработчиков за считанные дни, минуя долгий процесс классического рекрутинга и адаптации.", null);
            w.add("outstaff-page-use-case-items", "item-2", "tag", "TEXT", "Карточка 2: tag", "# Экономия времени", null);
            w.add("outstaff-page-use-case-items", "item-2", "title", "TEXT", "Карточка 2: title", "Нет времени на поиск и оформление в штат", null);
            w.add("outstaff-page-use-case-items", "item-2", "description", "TEXT", "Карточка 2: description", "Полностью берем на себя поиск, проверку компетенций, юридическое сопровождение и кадровое администрирование.", null);
            w.add("outstaff-page-use-case-items", "item-3", "tag", "TEXT", "Карточка 3: tag", "# Специфические навыки", null);
            w.add("outstaff-page-use-case-items", "item-3", "title", "TEXT", "Карточка 3: title", "Требуется узкая техническая экспертиза", null);
            w.add("outstaff-page-use-case-items", "item-3", "description", "TEXT", "Карточка 3: description", "Точечно подберем инженеров нужного грейда строго под ваш технологический стек и специфику текущих задач.", null);
            w.add("outstaff-page-use-case-items", "item-4", "tag", "TEXT", "Карточка 4: tag", "# Гибкий бюджет", null);
            w.add("outstaff-page-use-case-items", "item-4", "title", "TEXT", "Карточка 4: title", "Проект растет, но бюджет на штат ограничен", null);
            w.add("outstaff-page-use-case-items", "item-4", "description", "TEXT", "Карточка 4: description", "Позволяет оперативно наращивать или сокращать ресурсы разработки без увеличения постоянных расходов компании.", null);
            w.add("outstaff-page-use-case-items", "item-5", "tag", "TEXT", "Карточка 5: tag", "# Временные рамки", null);
            w.add("outstaff-page-use-case-items", "item-5", "title", "TEXT", "Карточка 5: title", "Проект ограничен по сроку", null);
            w.add("outstaff-page-use-case-items", "item-5", "description", "TEXT", "Карточка 5: description", "Подключаем профильных специалистов под конкретную фазу продукта, сложный релиз или определенный пул задач.", null);
            w.add("outstaff-page-specialists", null, "eyebrow", "TEXT", "Надзаголовок", "Предоставление специалистов позволяет гибко управлять ресурсами и ускорять реализацию цифровых проектов.", null);
            w.add("outstaff-page-specialists", null, "title", "TEXT", "Заголовок", "Наши специалисты", null);
            w.add("outstaff-page-specialists", null, "description", "TEXT", "Описание", "Предоставляем разработчиков, тестировщиков, аналитиков и инженеров по сопровождению инфраструктуры для усиления вашей команды.", null);
            w.add("outstaff-page-filters", "filter-all", "key", "TEXT", "Ключ фильтра", "all", null);
            w.add("outstaff-page-filters", "filter-all", "label", "TEXT", "Название фильтра", "Все", null);
            w.add("outstaff-page-filters", "filter-web", "key", "TEXT", "Ключ фильтра", "web", null);
            w.add("outstaff-page-filters", "filter-web", "label", "TEXT", "Название фильтра", "Web разработка", null);
            w.add("outstaff-page-filters", "filter-mobile", "key", "TEXT", "Ключ фильтра", "mobile", null);
            w.add("outstaff-page-filters", "filter-mobile", "label", "TEXT", "Название фильтра", "Mobile разработка", null);
            w.add("outstaff-page-filters", "filter-automation", "key", "TEXT", "Ключ фильтра", "automation", null);
            w.add("outstaff-page-filters", "filter-automation", "label", "TEXT", "Название фильтра", "Цифровая автоматизация", null);
            w.add("outstaff-page-filters", "filter-crm", "key", "TEXT", "Ключ фильтра", "crm", null);
            w.add("outstaff-page-filters", "filter-crm", "label", "TEXT", "Название фильтра", "Интеграция и разработка CRM систем", null);
            w.add("outstaff-page-filters", "filter-1c", "key", "TEXT", "Ключ фильтра", "1c", null);
            w.add("outstaff-page-filters", "filter-1c", "label", "TEXT", "Название фильтра", "Услуги в области 1С интеграций", null);
            w.add("outstaff-page-specialist-items", "specialist-1", "direction", "TEXT", "Специалист 1: direction", "Design", null);
            w.add("outstaff-page-specialist-items", "specialist-1", "grade", "TEXT", "Специалист 1: grade", "Middle", null);
            w.add("outstaff-page-specialist-items", "specialist-1", "title", "TEXT", "Специалист 1: title", "Дизайнер пользовательского интерфейса (UX/UI)", null);
            w.add("outstaff-page-specialist-items", "specialist-1", "skills", "TEXT", "Специалист 1: skills", "CJM • Количественные/Качественные исследования • Протопипирование", null);
            w.add("outstaff-page-specialist-items", "specialist-1", "price", "TEXT", "Специалист 1: price", "от 2.000 ₽/час", null);
            w.add("outstaff-page-specialist-items", "specialist-1", "filter1", "TEXT", "filter1", "web", null);
            w.add("outstaff-page-specialist-items", "specialist-1", "filter2", "TEXT", "filter2", "mobile", null);
            w.add("outstaff-page-specialist-items", "specialist-1", "cta", "LINK", "cta", "Заказать услугу", "/outstaff#lead-form");
            w.add("outstaff-page-specialist-items", "specialist-2", "direction", "TEXT", "Специалист 2: direction", "Design", null);
            w.add("outstaff-page-specialist-items", "specialist-2", "grade", "TEXT", "Специалист 2: grade", "Senior", null);
            w.add("outstaff-page-specialist-items", "specialist-2", "title", "TEXT", "Специалист 2: title", "Дизайнер пользовательского интерфейса (UX/UI)", null);
            w.add("outstaff-page-specialist-items", "specialist-2", "skills", "TEXT", "Специалист 2: skills", "CJM • Количественные/Качественные исследования • Протопипирование", null);
            w.add("outstaff-page-specialist-items", "specialist-2", "price", "TEXT", "Специалист 2: price", "от 2.800 ₽/час", null);
            w.add("outstaff-page-specialist-items", "specialist-2", "filter1", "TEXT", "filter1", "web", null);
            w.add("outstaff-page-specialist-items", "specialist-2", "filter2", "TEXT", "filter2", "mobile", null);
            w.add("outstaff-page-specialist-items", "specialist-2", "cta", "LINK", "cta", "Заказать услугу", "/outstaff#lead-form");
            w.add("outstaff-page-specialist-items", "specialist-3", "direction", "TEXT", "Специалист 3: direction", "Front-end", null);
            w.add("outstaff-page-specialist-items", "specialist-3", "grade", "TEXT", "Специалист 3: grade", "Middle", null);
            w.add("outstaff-page-specialist-items", "specialist-3", "title", "TEXT", "Специалист 3: title", "Клиентская разработка (Front-end)", null);
            w.add("outstaff-page-specialist-items", "specialist-3", "skills", "TEXT", "Специалист 3: skills", "Vue.js • Nuxt.js • JavaScript • TypeScript • HTML • CSS", null);
            w.add("outstaff-page-specialist-items", "specialist-3", "price", "TEXT", "Специалист 3: price", "от 2.000 ₽/час", null);
            w.add("outstaff-page-specialist-items", "specialist-3", "filter1", "TEXT", "filter1", "web", null);
            w.add("outstaff-page-specialist-items", "specialist-3", "filter2", "TEXT", "filter2", "mobile", null);
            w.add("outstaff-page-specialist-items", "specialist-3", "cta", "LINK", "cta", "Заказать услугу", "/outstaff#lead-form");
            w.add("outstaff-page-specialist-items", "specialist-4", "direction", "TEXT", "Специалист 4: direction", "Back-end", null);
            w.add("outstaff-page-specialist-items", "specialist-4", "grade", "TEXT", "Специалист 4: grade", "Middle", null);
            w.add("outstaff-page-specialist-items", "specialist-4", "title", "TEXT", "Специалист 4: title", "Серверная разработка (Back-end)", null);
            w.add("outstaff-page-specialist-items", "specialist-4", "skills", "TEXT", "Специалист 4: skills", "Laravel • 1С-Битрикс • MySQL • PostgreSQL • REST API", null);
            w.add("outstaff-page-specialist-items", "specialist-4", "price", "TEXT", "Специалист 4: price", "от 2.000 ₽/час", null);
            w.add("outstaff-page-specialist-items", "specialist-4", "grade2", "TEXT", "grade2", "Team lead", null);
            w.add("outstaff-page-specialist-items", "specialist-4", "filter1", "TEXT", "filter1", "web", null);
            w.add("outstaff-page-specialist-items", "specialist-4", "filter2", "TEXT", "filter2", "crm", null);
            w.add("outstaff-page-specialist-items", "specialist-4", "filter3", "TEXT", "filter3", "1c", null);
            w.add("outstaff-page-specialist-items", "specialist-4", "cta", "LINK", "cta", "Заказать услугу", "/outstaff#lead-form");
            w.add("outstaff-page-specialist-items", "specialist-5", "direction", "TEXT", "Специалист 5: direction", "DevOps", null);
            w.add("outstaff-page-specialist-items", "specialist-5", "grade", "TEXT", "Специалист 5: grade", "Руководитель направления", null);
            w.add("outstaff-page-specialist-items", "specialist-5", "title", "TEXT", "Специалист 5: title", "Инженер по сопровождению инфраструктуры (DevOps)", null);
            w.add("outstaff-page-specialist-items", "specialist-5", "skills", "TEXT", "Специалист 5: skills", "Docker • Kubernetes • Terraform • Jenkins", null);
            w.add("outstaff-page-specialist-items", "specialist-5", "price", "TEXT", "Специалист 5: price", "от 2.000 ₽/час", null);
            w.add("outstaff-page-specialist-items", "specialist-5", "filter1", "TEXT", "filter1", "automation", null);
            w.add("outstaff-page-specialist-items", "specialist-5", "filter2", "TEXT", "filter2", "crm", null);
            w.add("outstaff-page-specialist-items", "specialist-5", "cta", "LINK", "cta", "Заказать услугу", "/outstaff#lead-form");
            w.add("outstaff-page-specialist-items", "specialist-6", "direction", "TEXT", "Специалист 6: direction", "DevOps", null);
            w.add("outstaff-page-specialist-items", "specialist-6", "grade", "TEXT", "Специалист 6: grade", "Руководитель направления", null);
            w.add("outstaff-page-specialist-items", "specialist-6", "title", "TEXT", "Специалист 6: title", "Автоматизированное тестирование", null);
            w.add("outstaff-page-specialist-items", "specialist-6", "skills", "TEXT", "Специалист 6: skills", "JavaScript • PHP • Vue.js • Node.js", null);
            w.add("outstaff-page-specialist-items", "specialist-6", "price", "TEXT", "Специалист 6: price", "от 2.000 ₽/час", null);
            w.add("outstaff-page-specialist-items", "specialist-6", "filter1", "TEXT", "filter1", "automation", null);
            w.add("outstaff-page-specialist-items", "specialist-6", "filter2", "TEXT", "filter2", "1c", null);
            w.add("outstaff-page-specialist-items", "specialist-6", "cta", "LINK", "cta", "Заказать услугу", "/outstaff#lead-form");
            w.add("outstaff-page-team", null, "count", "TEXT", "count", "70+", null);
            w.add("outstaff-page-team", null, "description", "TEXT", "Описание", "И еще более 70 крутых специалистов в команде", null);
            w.add("outstaff-page-team-overrides", "member-6", "name", "TEXT", "Имя участника команды на Outstaff", "Алёша", null);
            w.add("outstaff-page-team-overrides", "member-6", "position", "TEXT", "Должность участника команды на Outstaff", "React-разработчик", null);
            w.add("outstaff-page-process", null, "eyebrow", "TEXT", "Надзаголовок", "Процесс работы", null);
            w.add("outstaff-page-process", null, "title", "TEXT", "Заголовок", "Как работает модель сотрудничества", null);
            w.add("outstaff-page-process-steps", "item-1", "number", "TEXT", "Карточка 1: number", "1", null);
            w.add("outstaff-page-process-steps", "item-1", "title", "TEXT", "Карточка 1: title", "Совместно определяем требования к специалисту", null);
            w.add("outstaff-page-process-steps", "item-1", "description", "TEXT", "Карточка 1: description", "Фиксируем стек технологий, уровень, формат занятости и рабочий контур проекта.", null);
            w.add("outstaff-page-process-steps", "item-2", "number", "TEXT", "Карточка 2: number", "2", null);
            w.add("outstaff-page-process-steps", "item-2", "title", "TEXT", "Карточка 2: title", "Подбираем кандидата и организуем техническое интервью", null);
            w.add("outstaff-page-process-steps", "item-2", "description", "TEXT", "Карточка 2: description", "Показываем релевантных специалистов и согласуем финальный выбор с вашей командой.", null);
            w.add("outstaff-page-process-steps", "item-3", "number", "TEXT", "Карточка 3: number", "3", null);
            w.add("outstaff-page-process-steps", "item-3", "title", "TEXT", "Карточка 3: title", "Заключаем договор с прозрачными условиями сотрудничества", null);
            w.add("outstaff-page-process-steps", "item-3", "description", "TEXT", "Карточка 3: description", "Согласуем формат отчетности, ставку, объем часов и правила замены при необходимости.", null);
            w.add("outstaff-page-process-steps", "item-4", "number", "TEXT", "Карточка 4: number", "4", null);
            w.add("outstaff-page-process-steps", "item-4", "title", "TEXT", "Карточка 4: title", "Специалист подключается к вашему проекту", null);
            w.add("outstaff-page-process-steps", "item-4", "description", "TEXT", "Карточка 4: description", "Работает в составе вашей команды и быстро включается в текущий производственный процесс.", null);
            w.add("outstaff-page-process-steps", "item-5", "number", "TEXT", "Карточка 5: number", "5", null);
            w.add("outstaff-page-process-steps", "item-5", "title", "TEXT", "Карточка 5: title", "Ведем учет рабочего времени и даем отчетность", null);
            w.add("outstaff-page-process-steps", "item-5", "description", "TEXT", "Карточка 5: description", "Вы получаете регулярную информацию по выполненным задачам и затраченным часам.", null);
            w.add("outstaff-page-terms", null, "title", "TEXT", "Заголовок", "Условия и форматы оплаты", null);
            w.add("outstaff-page-term-items", "item-1", "title", "TEXT", "Карточка 1: title", "Постоплата (Time & Material)", null);
            w.add("outstaff-page-term-items", "item-1", "description", "TEXT", "Карточка 1: description", "Оплата за фактически отработанные часы специалистов по итогам месяца.", null);
            w.add("outstaff-page-term-items", "item-2", "title", "TEXT", "Карточка 2: title", "Гибкие графики", null);
            w.add("outstaff-page-term-items", "item-2", "description", "TEXT", "Карточка 2: description", "Возможность индивидуальной настройки периодов выплат (от ежемесячных платежей до расчетов раз в квартал для крупных проектов).", null);
            w.add("outstaff-page-term-items", "item-3", "title", "TEXT", "Карточка 3: title", "Работа без предоплаты", null);
            w.add("outstaff-page-term-items", "item-3", "description", "TEXT", "Карточка 3: description", "Запуск проверенных партнеров и старт пула задач сразу после подписания договора, без ожидания траншей.", null);
            w.add("outstaff-page-contact", null, "specialist", "TEXT", "specialist", "Какой специалист нужен", null);
            w.add("outstaff-page-contact", null, "grade", "TEXT", "grade", "Уровень грейда", null);
            w.add("outstaff-page-contact", null, "hint", "TEXT", "hint", "например, какую основную проблему вам нужно решить, поделитесь целью, которую хотите достичь", null);
            w.add("outstaff-page-contact-specialists", "option-1", "label", "TEXT", "label", "UX/UI Designer", null);
            w.add("outstaff-page-contact-specialists", "option-2", "label", "TEXT", "label", "Front-end разработчик", null);
            w.add("outstaff-page-contact-specialists", "option-3", "label", "TEXT", "label", "Back-end разработчик", null);
            w.add("outstaff-page-contact-specialists", "option-4", "label", "TEXT", "label", "DevOps-инженер", null);
            w.add("outstaff-page-contact-specialists", "option-5", "label", "TEXT", "label", "Инженер по автоматизированному тестированию", null);
            w.add("outstaff-page-contact-specialists", "option-6", "label", "TEXT", "label", "Системный аналитик", null);
            w.add("outstaff-page-contact-grades", "junior", "label", "TEXT", "label", "junior", null);
            w.add("outstaff-page-contact-grades", "middle", "label", "TEXT", "label", "middle", null);
            w.add("outstaff-page-contact-grades", "senior", "label", "TEXT", "label", "senior", null);
            w.add("outstaff-page-seo", null, "eyebrow", "TEXT", "Надзаголовок", "Для поискового интента", null);
            w.add("outstaff-page-seo", null, "title", "TEXT", "Заголовок", "Аутстаффинг IT-специалистов в Москве под ключ: от подбора до интеграции в проект", null);
            w.add("outstaff-page-seo", null, "paragraph1", "TEXT", "paragraph1", "Студия ITWEBS предоставляет услуги профессионального аутстаффинга квалифицированных IT-кадров для малого, среднего и крупного бизнеса. Мы предлагаем индивидуальный подбор опытных разработчиков, DevOps-инженеров, системных аналитиков и тестировщиков под конкретные задачи вашего проекта. Наши специалисты обладают глубокими техническими компетенциями и готовы оперативно включиться в работу, что гарантирует высокую скорость масштабирования вашей команды и непрерывность разработки.", null);
            w.add("outstaff-page-seo", null, "paragraph2", "TEXT", "paragraph2", "Наша команда обеспечивает полное юридическое сопровождение, кадровый учет и расчет заработных плат привлеченных специалистов, полностью оптимизируя ваши операционные расходы. Мы настраиваем бесшовную интеграцию сотрудников в существующие бизнес-процессы и методологии управления (Agile, Scrum, Kanban). Обеспечиваем строгий контроль качества выполнения задач, прозрачную отчетность, а также полную конфиденциальность и безопасность ваших данных.", null);
            w.add("outstaff-page-seo", null, "subtitle", "TEXT", "Подзаголовок", "Комплексные кадровые решения для роста вашего бизнеса", null);
            w.add("outstaff-page-seo-items", "item-1", "label", "TEXT", "Карточка 1: label", "Выделенные IT-команды", null);
            w.add("outstaff-page-seo-items", "item-2", "label", "TEXT", "Карточка 2: label", "Быстрое масштабирование штата", null);
            w.add("outstaff-page-seo-items", "item-3", "label", "TEXT", "Карточка 3: label", "Снижение налоговых рисков", null);
            w.add("outstaff-page-seo-items", "item-4", "label", "TEXT", "Карточка 4: label", "Управление проектными рисками", null);
            w.add("_system", null, "outstaffPageSeedV1", "TEXT", "Outstaff seed", "complete", null);
        };
    }

    private static class Writer {
        private final ContentRepository repository;
        private final Set<String> existing = new HashSet<>();
        private int nextOrder;
        Writer(ContentRepository repository) {
            this.repository = repository;
            for (ContentItem item : repository.findAll()) {
                existing.add(identity(item.getSectionName(), item.getGroupName(), item.getItemKey()));
                nextOrder = Math.max(nextOrder, item.getSortOrder() + 1);
            }
        }
        private String identity(String section, String group, String key) {
            return section + "\u0000" + (group == null ? "" : group) + "\u0000" + key;
        }
        void add(String section, String group, String key, String type, String label, String value, String href) {
            if (!existing.add(identity(section, group, key))) return;
            ContentItem item = new ContentItem();
            item.setSectionName(section);
            item.setGroupName(group);
            item.setItemKey(key);
            item.setType(type);
            item.setLabel(label);
            item.setValue(value);
            item.setHref(href);
            item.setSortOrder(nextOrder++);
            item.setPublished(!section.equals("_system"));
            repository.save(item);
        }
    }
}
