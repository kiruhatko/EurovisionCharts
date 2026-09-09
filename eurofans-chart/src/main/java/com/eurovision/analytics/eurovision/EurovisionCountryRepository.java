package com.eurovision.analytics.eurovision;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EurovisionCountryRepository extends JpaRepository<EurovisionCountry, Long> {

    Optional<EurovisionCountry> findByIsoCode(String isoCode);
}
