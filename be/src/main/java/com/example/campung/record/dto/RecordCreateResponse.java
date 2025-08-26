package com.example.campung.record.dto;

public class RecordCreateResponse {
    private boolean success;
    private String message;
    private Long recordId;

    public RecordCreateResponse() {}

    public RecordCreateResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public RecordCreateResponse(boolean success, String message, Long recordId) {
        this.success = success;
        this.message = message;
        this.recordId = recordId;
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

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }
}