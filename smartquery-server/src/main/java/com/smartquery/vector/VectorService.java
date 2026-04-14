package com.smartquery.vector;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Service
public class VectorService {

    private final LuceneIndexService luceneIndexService;

    public VectorService(LuceneIndexService luceneIndexService) {
        this.luceneIndexService = luceneIndexService;
    }

    public String indexDocument(String deviceId, IndexRequest req) {
        var doc = new LuceneIndexService.VectorDocumentRequest(
                null,
                deviceId,
                req.filePath(),
                req.fileName(),
                req.mimeType(),
                String.valueOf(req.fileSize()),
                Instant.now().toString(),
                req.vector()
        );
        try {
            return luceneIndexService.addDocument(doc);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "index write failed", e);
        }
    }

    public void deleteById(String id) {
        try {
            luceneIndexService.deleteById(id);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "index delete failed", e);
        }
    }

    public void deleteByDeviceId(String deviceId) {
        try {
            luceneIndexService.deleteByDeviceId(deviceId);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "index delete failed", e);
        }
    }

    public List<LuceneIndexService.SearchResult> search(byte[] queryVector, int k) {
        try {
            return luceneIndexService.search(queryVector, k);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "search failed", e);
        }
    }

    public record IndexRequest(
            String filePath,
            String fileName,
            String mimeType,
            long fileSize,
            byte[] vector
    ) {}
}
