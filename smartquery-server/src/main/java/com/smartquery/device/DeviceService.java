package com.smartquery.device;

import com.smartquery.model.ApiKey;
import com.smartquery.model.ApiKeyRepository;
import com.smartquery.vector.LuceneIndexService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final LuceneIndexService luceneIndexService;

    public DeviceService(DeviceRepository deviceRepository,
                         ApiKeyRepository apiKeyRepository,
                         LuceneIndexService luceneIndexService) {
        this.deviceRepository = deviceRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.luceneIndexService = luceneIndexService;
    }

    /** Creates a device and its first API key. Returns the plain-text key (shown once). */
    @Transactional
    public DeviceRegistrationResult registerDevice(String userId, String name, Integer streamPort) {
        String deviceId = UUID.randomUUID().toString();
        String now = Instant.now().toString();

        Device device = new Device(deviceId, userId, name, now, null, streamPort);
        deviceRepository.save(device);

        String plainKey = generateRawKey();
        String keyHash = sha256Hex(plainKey);

        ApiKey apiKey = new ApiKey(
                UUID.randomUUID().toString(),
                deviceId,
                keyHash,
                name + " default key",
                null,
                now
        );
        apiKeyRepository.save(apiKey);

        return new DeviceRegistrationResult(deviceId, plainKey);
    }

    public List<Device> listDevices(String userId) {
        return deviceRepository.findAllByUserId(userId);
    }

    /** Deletes a device, revokes its API keys, and purges its Lucene documents. */
    @Transactional
    public void deleteDevice(String deviceId, String userId) {
        deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "device not found or does not belong to caller"));

        apiKeyRepository.deleteAllByDeviceId(deviceId);
        try {
            luceneIndexService.deleteByDeviceId(deviceId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "failed to purge Lucene index for device");
        }
        deviceRepository.deleteById(deviceId);
    }

    private String generateRawKey() {
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    public record DeviceRegistrationResult(String deviceId, String apiKey) {}
}
