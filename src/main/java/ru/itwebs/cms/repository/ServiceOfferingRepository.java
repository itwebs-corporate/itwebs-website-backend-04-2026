package ru.itwebs.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itwebs.cms.entity.ServiceOffering;
import java.util.List;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {
    List<ServiceOffering> findAllByOrderByIdAsc();
}
