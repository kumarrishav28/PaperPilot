package com.PaperPilot.controller;

import com.PaperPilot.dto.ApiFromResponse;
import com.PaperPilot.dto.DocMetaDTO;
import com.PaperPilot.dto.DocumentResponseDTO;
import com.PaperPilot.entity.Document;
import com.PaperPilot.service.DocumentProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Document Management", description = "APIs for managing documents")
public class DocumentController {

    private final DocumentProcessingService documentProcessingService;

    public DocumentController(DocumentProcessingService documentProcessingService) {
        this.documentProcessingService = documentProcessingService;
    }

    @PostMapping(value = "/upload" , consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and index document", description = "Uploads a document and indexes it for future retrieval and analysis.")
    public ResponseEntity<ApiFromResponse<DocumentResponseDTO>> uploadDocument(@RequestParam("document") MultipartFile document) {
        DocumentResponseDTO documentResponseDTO= documentProcessingService.UploadAndProcessDocument(document);
        ApiFromResponse<DocumentResponseDTO> apiResponse = ApiFromResponse.<DocumentResponseDTO>builder()
                .success(true)
                .message("Document uploaded successfully")
                .data(documentResponseDTO)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/upload/multiple")
    @Operation(summary = "Upload and index multiple documents", description = "Uploads multiple documents and indexes them for future retrieval and analysis.")
    public ResponseEntity<ApiFromResponse<List<DocumentResponseDTO>>> uploadMultipleDocuments(@RequestParam("documents") MultipartFile[] documents) {
        List<DocumentResponseDTO> documentResponseDTOs = documentProcessingService.uploadMultipleDocuments(documents);
        ApiFromResponse<List<DocumentResponseDTO>> apiResponse = ApiFromResponse.<List<DocumentResponseDTO>>builder()
                .success(true)
                .message("Documents uploaded successfully")
                .data(documentResponseDTOs)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all documents", description = "Retrieves a list of all documents.")
    public ResponseEntity<ApiFromResponse<List<DocMetaDTO>>> getAllDocuments() {
        List<DocMetaDTO> documents = documentProcessingService.getAllDocuments();
        ApiFromResponse<List<DocMetaDTO>> apiResponse = ApiFromResponse.<List<DocMetaDTO>>builder()
                .success(true)
                .message("Documents retrieved successfully")
                .data(documents)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/delete/{documentId}")
    @Operation(summary = "Delete a document", description = "Deletes a document by its ID.")
    public ResponseEntity<ApiFromResponse<Void>> deleteDocument(@PathVariable UUID documentId) {
        documentProcessingService.deleteDocument(documentId);
        ApiFromResponse<Void> apiResponse = ApiFromResponse.<Void>builder()
                .success(true)
                .message("Document deleted successfully")
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/fetch/{documentId}")
    @Operation(summary = "Get document by ID", description = "Retrieves a document's metadata by its ID.")
    public ResponseEntity<ApiFromResponse<DocMetaDTO>> getDocumentById(@PathVariable UUID documentId) {
        DocMetaDTO document = documentProcessingService.getDocumentById(documentId);
        ApiFromResponse<DocMetaDTO> apiResponse = ApiFromResponse.<DocMetaDTO>builder()
                .success(true)
                .message("Document retrieved successfully")
                .data(document)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.ok(apiResponse);
    }



}
