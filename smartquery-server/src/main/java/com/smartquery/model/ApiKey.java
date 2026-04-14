package com.smartquery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @Column(nullable = false)
    private String id;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "key_hash", unique = true, nullable = false)
    private String keyHash;

    @Column
    private String name;

    @Column(name = "expires_at")
    private String expiresAt;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    public ApiKey() {}

    public ApiKey(String id, String deviceId, String keyHash, String name, String expiresAt, String createdAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.keyHash = keyHash;
        this.name = name;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
