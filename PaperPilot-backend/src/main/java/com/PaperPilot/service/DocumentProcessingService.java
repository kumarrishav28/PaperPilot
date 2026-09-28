package com.PaperPilot.service;

import com.PaperPilot.dto.DocMetaDTO;
import com.PaperPilot.dto.DocumentResponseDTO;
import com.PaperPilot.entity.DocStatus;
import com.PaperPilot.entity.Document;
import com.PaperPilot.exception.DocumentProcessingException;
import com.PaperPilot.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final DocumentRepository documentRepository;

    private final JdbcTemplate jdbcTemplate;

    private final ParseDocumentService parseDocumentService;

    private final InjestionService injestionService;

    public DocumentResponseDTO UploadAndProcessDocument(MultipartFile document) {
        if (document != null) {
            String filename = null != document.getOriginalFilename() ? document.getOriginalFilename() : "unknown";
            String contentType = null != document.getContentType() ? document.getContentType() : "unknown";


            // STEP 1: Save the document metadata to the database with status UPLOADING
            Document doc = Document.builder()
                    .filename(filename)
                    .contentType(contentType)
                    .fileSize(document.getSize())
                    .status(DocStatus.UPLOADING)
                    .createdAt(LocalDateTime.now())
                    .build();
            documentRepository.save(doc);

            int chunkCreated = 0;
            try {
                // STEP 2 : PARSE THE DOCUMENT
                List<org.springframework.ai.document.Document> parsedDocument =
                        parseDocumentService.parse(document);

                // STEP 3: ingestion of the parsed document into the database or any other storage system
                chunkCreated = injestionService.ingestDocument(doc, parsedDocument);

            } catch (Exception e) {
                logger.error("Error processing document {}: {}", filename, e.getMessage());
                doc.setStatus(DocStatus.FAILED);
                documentRepository.delete(doc); // delete the document metadata from the database in case of failure
                throw new DocumentProcessingException("Error processing document " + filename, e);
            }

            return DocumentResponseDTO.builder()
                    .documentId(doc.getId())
                    .documentName(doc.getFilename())
                    .documentStatus(doc.getStatus())
                    .fileSize(doc.getFileSize())
                    .chunkCreated(chunkCreated)
                    .message("Document uploaded and processed successfully")
                    .build();

        }

        return DocumentResponseDTO.builder()
                .documentId(null)
                .documentName(null)
                .documentStatus(DocStatus.FAILED)
                .fileSize(0)
                .chunkCreated(0)
                .message("Document upload failed")
                .build();

    }

    /**
     * Upload and process multiple documents.
     *
     * @param documents An array of MultipartFile objects representing the documents to be uploaded and processed.
     * @return A list of DocumentResponseDTO objects containing the results of the upload and processing for each document.
     */
    public List<DocumentResponseDTO> uploadMultipleDocuments(MultipartFile[] documents) {
        return Stream.of(documents)
                .map(this::UploadAndProcessDocument)
                .toList();
    }


    /**
     * Retrieve all documents from the database and map them to DocMetaDTO objects.
     *
     * @return A list of DocMetaDTO objects containing metadata for all documents.
     */
    public List<DocMetaDTO> getAllDocuments() {
        List<Document> documents = documentRepository.findAllByOrderByCreatedAtDesc();
        return documents.stream()
                .map(doc -> DocMetaDTO.builder()
                        .id(doc.getId())
                        .filename(doc.getFilename())
                        .contentType(doc.getContentType())
                        .fileSize(doc.getFileSize())
                        .totalPages(doc.getTotalPages())
                        .totalChunks(doc.getTotalChunks())
                        .status(doc.getStatus())
                        .errorMessage(doc.getErrorMessage())
                        .createdAt(doc.getCreatedAt())
                        .updatedAt(doc.getUpdatedAt())
                        .build())
                .toList();
    }

    /**
     * Delete a document by its ID, including its metadata and associated vector embeddings.
     *
     * @param documentId The UUID of the document to be deleted.
     */
    public void deleteDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));
        documentRepository.delete(document);

        // delete vector embeddings associated with the document from the vector store
        try {
            // this query assumes that the vector embeddings are stored in a table named 'vector_store' and that the documentId is stored in the metadata JSON column.
            String deleteVectorsSql = "DELETE FROM vector_store WHERE metadata->>'documentId' = ?";
            jdbcTemplate.update(deleteVectorsSql, documentId.toString());
        } catch (Exception e) {
            logger.error("Error deleting vector embeddings for documentId {}: {}", documentId, e.getMessage());
        }
    }

    /**
     * Retrieve a document's metadata by its ID and map it to a DocMetaDTO object.
     *
     * @param documentId The UUID of the document to be retrieved.
     * @return A DocMetaDTO object containing metadata for the specified document.
     */
    public DocMetaDTO getDocumentById(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));
        return DocMetaDTO.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .totalPages(document.getTotalPages())
                .totalChunks(document.getTotalChunks())
                .status(document.getStatus())
                .errorMessage(document.getErrorMessage())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }


}
