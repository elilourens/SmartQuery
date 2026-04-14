package com.smartquery.vector;

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
@RequestMapping("/api/vectors")
public class VectorController {

    private final VectorService vectorService;

    public VectorController(VectorService vectorService) {
        this.vectorService = vectorService;
    }

    /** Index a new file document. The calling device is identified by its API key. */
    @PostMapping
    public ResponseEntity<?> indexDocument(@RequestBody IndexDocumentRequest body,
                                           @AuthenticationPrincipal String deviceId) {
        String id = vectorService.indexDocument(deviceId, new VectorService.IndexRequest(
                body.filePath(),
                body.fileName(),
                body.mimeType(),
                body.fileSize(),
                body.vector()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    /** Delete a single document by its Lucene id. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        vectorService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Delete all documents for the calling device. */
    @DeleteMapping("/device")
    public ResponseEntity<Void> deleteForDevice(@AuthenticationPrincipal String deviceId) {
        vectorService.deleteByDeviceId(deviceId);
        return ResponseEntity.noContent().build();
    }

    /** Nearest-neighbour search. */
    @GetMapping("/search")
    public List<SearchResultView> search(@RequestBody SearchRequest body) {
        return vectorService.search(body.vector(), body.k()).stream()
                .map(r -> new SearchResultView(
                        r.id(), r.deviceId(), r.filePath(), r.fileName(),
                        r.mimeType(), r.fileSize(), r.indexedAt(), r.score()))
                .toList();
    }

    // ---- Request / response records ----

    record IndexDocumentRequest(
            String filePath,
            String fileName,
            String mimeType,
            long fileSize,
            byte[] vector
    ) {}

    record SearchRequest(byte[] vector, int k) {}

    record SearchResultView(
            String id,
            String deviceId,
            String filePath,
            String fileName,
            String mimeType,
            String fileSize,
            String indexedAt,
            float score
    ) {}
}
