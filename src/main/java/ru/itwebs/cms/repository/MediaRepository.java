package ru.itwebs.cms.repository;

import ru.itwebs.cms.entity.MediaAsset;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<MediaAsset, Long> { }
