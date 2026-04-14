package com.smartquery.config;

import com.smartquery.device.DeviceService;
import com.smartquery.model.User;
import com.smartquery.model.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Seeds one user, one device, and a known API key on startup in the 'dev' profile.
 * Activate with: --spring.profiles.active=dev
 */
@Component
@Profile("dev")
public class DevSeedConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedConfig.class);

    private static final String SEED_EMAIL    = "dev@smartquery.local";
    private static final String SEED_PASSWORD = "devpassword";
    private static final String SEED_DEVICE   = "dev-laptop";

    private final UserRepository userRepository;
    private final DeviceService deviceService;
    private final PasswordEncoder passwordEncoder;

    public DevSeedConfig(UserRepository userRepository,
                         DeviceService deviceService,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.deviceService = deviceService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(SEED_EMAIL)) {
            log.info("[dev] Seed user already exists — skipping seed");
            return;
        }

        User user = new User(
                UUID.randomUUID().toString(),
                SEED_EMAIL,
                passwordEncoder.encode(SEED_PASSWORD),
                Instant.now().toString()
        );
        userRepository.save(user);

        DeviceService.DeviceRegistrationResult result =
                deviceService.registerDevice(user.getId(), SEED_DEVICE, 7070);

        log.info("=== DEV SEED (keep private) ===");
        log.info("  email    : {}", SEED_EMAIL);
        log.info("  password : {}", SEED_PASSWORD);
        log.info("  deviceId : {}", result.deviceId());
        log.info("  apiKey   : {}", result.apiKey());
        log.info("================================");
    }
}
