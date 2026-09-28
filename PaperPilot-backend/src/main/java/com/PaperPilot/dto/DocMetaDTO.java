package com.PaperPilot.dto;

import com.PaperPilot.entity.DocStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DocMetaDTO {
    private UUID id;
    private String filename;
    private String contentType;
    private Long fileSize;
    private Integer totalPages;
    private Integer totalChunks;
    private DocStatus status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
