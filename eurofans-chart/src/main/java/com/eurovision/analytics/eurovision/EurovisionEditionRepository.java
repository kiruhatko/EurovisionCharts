package com.eurovision.analytics.eurovision;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EurovisionEditionRepository extends JpaRepository<EurovisionEdition, Long> {

    Optional<EurovisionEdition> findByYear(int year);
}
