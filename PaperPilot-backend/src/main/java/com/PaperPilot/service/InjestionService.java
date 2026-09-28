package com.PaperPilot.service;

import com.PaperPilot.config.PaperPilotProperties;
import com.PaperPilot.entity.DocStatus;
import com.PaperPilot.entity.Document;
import com.PaperPilot.exception.DocumentProcessingException;
import com.PaperPilot.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class InjestionService {

    Logger logger = org.slf4j.LoggerFactory.getLogger(InjestionService.class);

    private final DocumentRepository documentRepository;

    private final VectorStore vectorStore;

    private final PaperPilotProperties appProperties;

    public Integer ingestDocument(Document document, List<org.springframework.ai.document.Document> parsedDocuments) {
        logger.warn("ingesting document with id: {} and name: {} and parsedDocuments size: {}", document.getId(), document.getFilename(), parsedDocuments.size());
        List<org.springframework.ai.document.Document> enrichedChunks;
        try {

            document.setStatus(DocStatus.PROCESSING);
            document.setTotalPages(parsedDocuments.size());

            // Step 1 : chunking using the TokenTextSplitter
            TokenTextSplitter tokenTextSplitter =
                    TokenTextSplitter.builder().withChunkSize(appProperties.getRag().getChunkSize())
                            .withMinChunkSizeChars(appProperties.getRag().getMinChunkSizeChars())
                            .withMinChunkLengthToEmbed(appProperties.getRag().getMinChunkLengthToEmbed())
                            .withMaxNumChunks(appProperties.getRag().getMaxNumChunks())
                            .withKeepSeparator(true)
                            .build();

            List<org.springframework.ai.document.Document> chunks = tokenTextSplitter.apply(parsedDocuments);
            if (chunks.isEmpty()) {
                logger.warn("No chunks were created for document with id: {} and name: {}", document.getId(), document.getFilename());
                document.setStatus(DocStatus.FAILED);
                document.setErrorMessage("No chunks were created for document with id: " + document.getId() + " and name: " + document.getFilename());
                documentRepository.save(document);
                return 0;
            }


            // step 2 : metadata enrichment at each chunk level
            enrichedChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                org.springframework.ai.document.Document chunk = chunks.get(i);
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("documentId", document.getId().toString());
                metadata.put("filename", document.getFilename());
                metadata.put("fileName", document.getFilename());
                metadata.put("contentType", document.getContentType());
                metadata.put("chunkIndex", i);
                //metadata.put("userId", document.getUser().getId());

                Object pageNumber = chunk.getMetadata().get("page_number");

                if (pageNumber == null) {
                    pageNumber = chunk.getMetadata().get("pageNumber");
                }

                metadata.put("pageNumber", pageNumber);
                org.springframework.ai.document.Document enrichedChunk = new org.springframework.ai.document.Document(chunk.getText(), metadata);
                enrichedChunks.add(enrichedChunk);

            }

            // step 3: embedding and storing in vector store
            logger.warn("Storing {} enriched chunks in vector store for document with id: {} and name: {}", enrichedChunks.size(), document.getId(), document.getFilename());
            vectorStore.add(enrichedChunks);

            // step 4: update document status to INDEXED
            document.setStatus(DocStatus.INDEXED);
            document.setTotalChunks(enrichedChunks.size());
            documentRepository.save(document);
            logger.info("Document with id: {} and name: {} has been successfully ingested and indexed with {} chunks", document.getId(), document.getFilename(), enrichedChunks.size());

        } catch (Exception e) {
            document.setStatus(DocStatus.FAILED);
            document.setErrorMessage("Error while ingesting document with id: " + document.getId() + " and name: " + document.getFilename() + ": " + e.getMessage());
            documentRepository.save(document);
            throw new DocumentProcessingException("Error while ingesting document with id: " + document.getId() + " and name: " + document.getFilename(), e);

        }
        return enrichedChunks.size();
    }
}
