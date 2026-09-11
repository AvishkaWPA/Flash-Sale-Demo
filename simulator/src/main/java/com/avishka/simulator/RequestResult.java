package com.avishka.simulator;

public class RequestResult {

    private final int userId;
    private final int statusCode;
    private final long latencyMs;
    private final boolean success;

    public RequestResult(
            int userId,
            int statusCode,
            long latencyMs,
            boolean success
    ) {
        this.userId = userId;
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
        this.success = success;
    }

    public int getUserId() {
        return userId;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public boolean isSuccess() {
        return success;
    }
}
