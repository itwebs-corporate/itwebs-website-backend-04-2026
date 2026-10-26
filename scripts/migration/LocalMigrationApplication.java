package ru.itwebs.cms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/** Local migration host: real API and persistence, without sample content or scheduled integrations. */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "ru.itwebs.cms", excludeFilters =
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = {
        "ru\\.itwebs\\.cms\\.bootstrap\\..*", "ru\\.itwebs\\.cms\\.SiteApplication"
    }))
public class LocalMigrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(LocalMigrationApplication.class, args);
    }
}
