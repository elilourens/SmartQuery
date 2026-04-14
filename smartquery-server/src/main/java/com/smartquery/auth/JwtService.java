package com.smartquery.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    @Value("${jwt.keys-dir}")
    private String keysDir;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() throws Exception {
        Path keysPath = Path.of(keysDir);
        Path privateKeyPath = keysPath.resolve("private.pem");
        Path publicKeyPath = keysPath.resolve("public.pem");

        if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
            privateKey = loadPrivateKey(privateKeyPath);
            publicKey = loadPublicKey(publicKeyPath);
        } else {
            KeyPair keyPair = generateRsaKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
            Files.createDirectories(keysPath);
            saveKey(privateKeyPath, privateKey.getEncoded(), "PRIVATE KEY");
            saveKey(publicKeyPath, publicKey.getEncoded(), "PUBLIC KEY");
        }
    }

    public String generateAccessToken(String userId) {
        return buildToken(userId, "access", accessTokenExpiryMs);
    }

    public String generateRefreshToken(String userId) {
        return buildToken(userId, "refresh", refreshTokenExpiryMs);
    }

    /** Returns the userId claim, or throws JwtException on invalid/expired token. */
    public String validateToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    private String buildToken(String userId, String type, long expiryMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId)
                .claim("type", type)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiryMs))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    private void saveKey(Path path, byte[] encoded, String label) throws IOException {
        String pem = "-----BEGIN " + label + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded)
                + "\n-----END " + label + "-----\n";
        Files.writeString(path, pem);
    }

    private PrivateKey loadPrivateKey(Path path) throws Exception {
        byte[] der = decodePem(Files.readString(path));
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private PublicKey loadPublicKey(Path path) throws Exception {
        byte[] der = decodePem(Files.readString(path));
        return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(der));
    }

    private byte[] decodePem(String pem) {
        String stripped = pem
                .replaceAll("-----BEGIN .*?-----", "")
                .replaceAll("-----END .*?-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(stripped);
    }
}
