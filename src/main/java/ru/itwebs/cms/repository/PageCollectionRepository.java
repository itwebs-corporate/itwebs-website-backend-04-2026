package ru.itwebs.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itwebs.cms.entity.PageCollection;

public interface PageCollectionRepository extends JpaRepository<PageCollection, Long> { }
