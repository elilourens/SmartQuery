package com.smartquery.device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @Column(nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "last_seen")
    private String lastSeen;

    @Column(name = "ip_hint")
    private String ipHint;

    @Column(name = "stream_port")
    private Integer streamPort;

    public Device() {}

    public Device(String id, String userId, String name, String lastSeen, String ipHint, Integer streamPort) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.lastSeen = lastSeen;
        this.ipHint = ipHint;
        this.streamPort = streamPort;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLastSeen() { return lastSeen; }
    public void setLastSeen(String lastSeen) { this.lastSeen = lastSeen; }

    public String getIpHint() { return ipHint; }
    public void setIpHint(String ipHint) { this.ipHint = ipHint; }

    public Integer getStreamPort() { return streamPort; }
    public void setStreamPort(Integer streamPort) { this.streamPort = streamPort; }
}
