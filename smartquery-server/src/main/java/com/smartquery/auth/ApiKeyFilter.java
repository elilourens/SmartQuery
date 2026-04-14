package com.smartquery.auth;

import com.smartquery.model.ApiKey;
import com.smartquery.model.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

public class ApiKeyFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String rawKey = extractKey(request);
        if (!StringUtils.hasText(rawKey)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String hash = sha256Hex(rawKey);
            Optional<ApiKey> apiKey = apiKeyRepository.findByKeyHash(hash);
            if (apiKey.isPresent()) {
                String deviceId = apiKey.get().getDeviceId();
                var auth = new UsernamePasswordAuthenticationToken(
                        deviceId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_DEVICE"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception ignored) {
            // Hash failure — leave context empty
        }

        chain.doFilter(request, response);
    }

    private String extractKey(HttpServletRequest request) {
        // Accept: X-Api-Key header OR Authorization: ApiKey <key>
        String header = request.getHeader("X-Api-Key");
        if (StringUtils.hasText(header)) {
            return header.trim();
        }
        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("ApiKey ")) {
            return auth.substring(7).trim();
        }
        return null;
    }

    private String sha256Hex(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
