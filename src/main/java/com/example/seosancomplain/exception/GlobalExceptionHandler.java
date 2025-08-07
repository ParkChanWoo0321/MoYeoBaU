package com.example.seosancomplain.exception;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<SimpleResponseDto> handleCustomException(CustomException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new SimpleResponseDto(false, e.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<SimpleResponseDto> handleException(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SimpleResponseDto(false, "알 수 없는 오류가 발생했습니다: " + e.getMessage()));
    }
}
