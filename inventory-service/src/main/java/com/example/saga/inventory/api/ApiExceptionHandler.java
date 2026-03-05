package com.example.saga.inventory.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {

  public record ErrorResponse(String error) {}

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
    return new ErrorResponse(ex.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleIllegalState(IllegalStateException ex) {
    return new ErrorResponse(ex.getMessage());
  }
}