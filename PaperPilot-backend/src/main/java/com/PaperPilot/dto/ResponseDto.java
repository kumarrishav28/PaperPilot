package com.PaperPilot.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseDto {

    private String response;
    private String conversationId;
    private List<CitationDto> citations;
    private Long responseTime;

}
