package ru.itwebs.cms.repository;

import ru.itwebs.cms.entity.MediaAsset;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<MediaAsset, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select m from MediaAsset m where m.id = :id")
    java.util.Optional<MediaAsset> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
