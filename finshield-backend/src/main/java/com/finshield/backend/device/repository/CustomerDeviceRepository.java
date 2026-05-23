package com.finshield.backend.device.repository;

import com.finshield.backend.device.domain.CustomerDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerDeviceRepository extends JpaRepository<CustomerDevice, UUID> {

    Page<CustomerDevice> findAllByCustomerId(UUID customerId, Pageable pageable);

    Optional<CustomerDevice> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<CustomerDevice> findByCustomerIdAndDeviceId(UUID customerId, String deviceId);

    boolean existsByCustomerIdAndDeviceId(UUID customerId, String deviceId);
}
