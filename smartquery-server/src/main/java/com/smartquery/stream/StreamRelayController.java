package com.smartquery.stream;

import com.smartquery.device.Device;
import com.smartquery.device.DeviceRepository;
import com.smartquery.vector.LuceneIndexService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/api/stream")
public class StreamRelayController {

    private final LuceneIndexService luceneIndexService;
    private final DeviceRepository deviceRepository;
    private final WebClient webClient;

    public StreamRelayController(LuceneIndexService luceneIndexService,
                                 DeviceRepository deviceRepository,
                                 WebClient.Builder webClientBuilder) {
        this.luceneIndexService = luceneIndexService;
        this.deviceRepository = deviceRepository;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Proxies a file from the origin device to the caller.
     *
     * Flow:
     *  1. Look up vectorId in Lucene → get deviceId + filePath
     *  2. Look up device in SQLite → get ip_hint + stream_port
     *  3. WebClient.GET http://ipHint:streamPort/stream?path=filePath
     *  4. Pipe the byte stream back to the HTTP response
     */
    @GetMapping(value = "/{vectorId}", produces = "application/octet-stream")
    public Flux<DataBuffer> relay(@PathVariable String vectorId, ServerWebExchange exchange) {
        LuceneIndexService.SearchResult meta;
        try {
            meta = luceneIndexService.getById(vectorId);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "index lookup failed");
        }
        if (meta == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "vector document not found");
        }

        Device device = deviceRepository.findById(meta.deviceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "origin device not registered"));

        if (device.getIpHint() == null || device.getStreamPort() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "origin device has no known IP or stream port");
        }

        String originUrl = "http://" + device.getIpHint() + ":" + device.getStreamPort()
                + "/stream?path=" + encodePathParam(meta.filePath());

        return webClient.get()
                .uri(URI.create(originUrl))
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.createException().map(e ->
                                new ResponseStatusException(HttpStatus.BAD_GATEWAY, "origin device error: " + e.getMessage())))
                .bodyToFlux(DataBuffer.class);
    }

    private String encodePathParam(String raw) {
        return java.net.URLEncoder.encode(raw, java.nio.charset.StandardCharsets.UTF_8);
    }
}
