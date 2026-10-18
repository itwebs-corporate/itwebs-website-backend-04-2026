package ru.itwebs.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itwebs.cms.entity.ServiceComponentLink;

public interface ServiceComponentLinkRepository extends JpaRepository<ServiceComponentLink, Long> {
    boolean existsByComponentId(Long id);
}
