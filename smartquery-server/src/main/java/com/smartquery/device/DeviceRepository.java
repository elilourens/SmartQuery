package com.smartquery.device;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, String> {

    List<Device> findAllByUserId(String userId);

    Optional<Device> findByIdAndUserId(String id, String userId);
}
