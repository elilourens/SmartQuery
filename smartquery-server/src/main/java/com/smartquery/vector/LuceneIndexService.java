package com.smartquery.vector;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.lucene.codecs.KnnVectorsFormat;
import org.apache.lucene.codecs.lucene101.Lucene101Codec;
import org.apache.lucene.codecs.lucene102.Lucene102HnswBinaryQuantizedVectorsFormat;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KnnByteVectorField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnByteVectorQuery;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LuceneIndexService {

    // Stored field names
    static final String F_ID        = "id";
    static final String F_DEVICE_ID = "deviceId";
    static final String F_FILE_PATH = "filePath";
    static final String F_FILE_NAME = "fileName";
    static final String F_MIME_TYPE = "mimeType";
    static final String F_FILE_SIZE = "fileSize";
    static final String F_INDEXED_AT = "indexedAt";
    static final String F_VECTOR    = "vector";

    @Value("${lucene.index.path}")
    private String indexPath;

    private FSDirectory directory;
    private IndexWriter writer;
    private SearcherManager searcherManager;

    @PostConstruct
    public void init() throws IOException {
        directory = FSDirectory.open(Path.of(indexPath));

        IndexWriterConfig config = new IndexWriterConfig()
                .setCodec(new Lucene101Codec() {
                    @Override
                    public KnnVectorsFormat getKnnVectorsFormatForField(String field) {
                        if (F_VECTOR.equals(field)) {
                            return new Lucene102HnswBinaryQuantizedVectorsFormat();
                        }
                        return super.getKnnVectorsFormatForField(field);
                    }
                });

        writer = new IndexWriter(directory, config);
        searcherManager = new SearcherManager(writer, null);
    }

    @PreDestroy
    public void close() throws IOException {
        if (searcherManager != null) searcherManager.close();
        if (writer != null) writer.close();
        if (directory != null) directory.close();
    }

    /**
     * Adds (or replaces by id) a document in the index.
     * If id is null, a new UUID is generated and returned.
     */
    public String addDocument(VectorDocumentRequest req) throws IOException {
        String id = req.id() != null ? req.id() : UUID.randomUUID().toString();

        // Remove any existing doc with this id first (upsert semantics)
        writer.deleteDocuments(new Term(F_ID, id));

        Document doc = new Document();
        doc.add(new StringField(F_ID,        id,            Field.Store.YES));
        doc.add(new StringField(F_DEVICE_ID, req.deviceId(), Field.Store.YES));
        doc.add(new StoredField(F_FILE_PATH, req.filePath()));
        doc.add(new StoredField(F_FILE_NAME, req.fileName()));
        doc.add(new StoredField(F_MIME_TYPE, req.mimeType()));
        doc.add(new StoredField(F_FILE_SIZE, req.fileSize()));
        doc.add(new StoredField(F_INDEXED_AT, req.indexedAt()));
        doc.add(new KnnByteVectorField(F_VECTOR, req.vector(), VectorSimilarityFunction.COSINE));

        writer.addDocument(doc);
        writer.flush();
        searcherManager.maybeRefresh();
        return id;
    }

    /** Deletes a single document by its stored id field. */
    public void deleteById(String id) throws IOException {
        writer.deleteDocuments(new Term(F_ID, id));
        writer.flush();
        searcherManager.maybeRefresh();
    }

    /** Deletes all documents belonging to a device. */
    public void deleteByDeviceId(String deviceId) throws IOException {
        writer.deleteDocuments(new Term(F_DEVICE_ID, deviceId));
        writer.flush();
        searcherManager.maybeRefresh();
    }

    /** Returns the top-k nearest neighbours for the given byte vector. */
    public List<SearchResult> search(byte[] queryVector, int k) throws IOException {
        searcherManager.maybeRefreshBlocking();
        IndexSearcher searcher = searcherManager.acquire();
        try {
            KnnByteVectorQuery query = new KnnByteVectorQuery(F_VECTOR, queryVector, k);
            TopDocs hits = searcher.search(query, k);

            List<SearchResult> results = new ArrayList<>(hits.scoreDocs.length);
            for (var scoreDoc : hits.scoreDocs) {
                Document doc = searcher.storedFields().document(scoreDoc.doc);
                results.add(new SearchResult(
                        doc.get(F_ID),
                        doc.get(F_DEVICE_ID),
                        doc.get(F_FILE_PATH),
                        doc.get(F_FILE_NAME),
                        doc.get(F_MIME_TYPE),
                        doc.get(F_FILE_SIZE),
                        doc.get(F_INDEXED_AT),
                        scoreDoc.score
                ));
            }
            return results;
        } finally {
            searcherManager.release(searcher);
        }
    }

    /** Fetches a single document by id — used by the stream relay to look up filePath/deviceId. */
    public SearchResult getById(String id) throws IOException {
        searcherManager.maybeRefreshBlocking();
        IndexSearcher searcher = searcherManager.acquire();
        try {
            TopDocs hits = searcher.search(new TermQuery(new Term(F_ID, id)), 1);
            if (hits.totalHits.value() == 0) return null;
            Document doc = searcher.storedFields().document(hits.scoreDocs[0].doc);
            return new SearchResult(
                    doc.get(F_ID),
                    doc.get(F_DEVICE_ID),
                    doc.get(F_FILE_PATH),
                    doc.get(F_FILE_NAME),
                    doc.get(F_MIME_TYPE),
                    doc.get(F_FILE_SIZE),
                    doc.get(F_INDEXED_AT),
                    1.0f
            );
        } finally {
            searcherManager.release(searcher);
        }
    }

    // ---- Inner records ----

    public record VectorDocumentRequest(
            String id,          // null → auto-generate
            String deviceId,
            String filePath,
            String fileName,
            String mimeType,
            String fileSize,
            String indexedAt,
            byte[] vector
    ) {}

    public record SearchResult(
            String id,
            String deviceId,
            String filePath,
            String fileName,
            String mimeType,
            String fileSize,
            String indexedAt,
            float  score
    ) {}
}
