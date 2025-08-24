package com.example.campung.record.dto;

public class RecordDeleteResponse {
    private boolean success;
    private String message;

    public RecordDeleteResponse() {}

    public RecordDeleteResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}