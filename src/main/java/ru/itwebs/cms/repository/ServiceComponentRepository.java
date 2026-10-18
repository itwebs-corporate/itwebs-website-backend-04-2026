package ru.itwebs.cms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import ru.itwebs.cms.catalog.ServiceComponentDtos.ComponentKind;
import ru.itwebs.cms.entity.ServiceComponent;
import java.util.Optional;

public interface ServiceComponentRepository extends JpaRepository<ServiceComponent, Long> {
    Page<ServiceComponent> findByKind(ComponentKind kind, Pageable pageable);
    Optional<ServiceComponent> findByFingerprint(String fingerprint);
    boolean existsByImageId(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ServiceComponent c where c.id = :id")
    Optional<ServiceComponent> findByIdForUpdate(@Param("id") Long id);
}
