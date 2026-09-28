package com.PaperPilot.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiFromResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
}
