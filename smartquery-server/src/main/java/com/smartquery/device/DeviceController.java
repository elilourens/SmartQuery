package com.smartquery.device;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerDevice(@RequestBody RegisterDeviceRequest body,
                                            @AuthenticationPrincipal String userId) {
        DeviceService.DeviceRegistrationResult result =
                deviceService.registerDevice(userId, body.name(), body.streamPort());
        // API key shown once — never recoverable
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "deviceId", result.deviceId(),
                "apiKey", result.apiKey()
        ));
    }

    @GetMapping
    public List<DeviceView> listDevices(@AuthenticationPrincipal String userId) {
        return deviceService.listDevices(userId).stream()
                .map(d -> new DeviceView(d.getId(), d.getName(), d.getLastSeen(), d.getStreamPort()))
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable String id,
                                             @AuthenticationPrincipal String userId) {
        deviceService.deleteDevice(id, userId);
        return ResponseEntity.noContent().build();
    }

    record RegisterDeviceRequest(String name, Integer streamPort) {}

    record DeviceView(String id, String name, String lastSeen, Integer streamPort) {}
}
