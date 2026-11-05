package com.hackathon.seoul.common;

import org.springframework.http.HttpStatus;

public class GameException extends RuntimeException {
    private final HttpStatus status;

    public GameException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
