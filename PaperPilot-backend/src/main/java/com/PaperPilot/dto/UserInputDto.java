package com.PaperPilot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@RequiredArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class UserInputDto {

    @NotBlank (message = "Query cannot be blank")
    private String query;

    private UUID documentId;

    private Integer topK;

    private Double similarityThreshold;

    private String conversationId;
}
