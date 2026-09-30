package ru.itwebs.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itwebs.cms.entity.AmoCrmConnection;

public interface AmoCrmConnectionRepository extends JpaRepository<AmoCrmConnection, Long> { }
