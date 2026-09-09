package com.eurovision.analytics.deployment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentBindingMetadataRepository extends JpaRepository<DeploymentBindingMetadata, Long> {
}
