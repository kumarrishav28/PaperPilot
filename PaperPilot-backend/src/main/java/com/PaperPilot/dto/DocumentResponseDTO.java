package com.PaperPilot.dto;

import com.PaperPilot.entity.DocStatus;
import lombok.*;

import java.util.UUID;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentResponseDTO {

    private UUID documentId;
    private String documentName;
    private DocStatus documentStatus;
    private long fileSize;
    private int chunkCreated;
    private String message;
    private Long userId;
}
