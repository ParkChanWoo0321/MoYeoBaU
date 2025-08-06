package com.example.seosancomplain.exception;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SimpleResponseDto {
    private boolean success;
    private String message;
}
