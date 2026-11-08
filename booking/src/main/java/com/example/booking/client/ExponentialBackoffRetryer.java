package com.example.booking.client;

import feign.RetryableException;
import feign.Retryer;

public final class ExponentialBackoffRetryer implements Retryer {

    private final long initialBackoffMillis;
    private final double multiplier;
    private final int maxAttempts;
    private int attempt = 1;

    public ExponentialBackoffRetryer(long initialBackoffMillis, double multiplier, int maxAttempts) {
        this.initialBackoffMillis = initialBackoffMillis;
        this.multiplier = multiplier;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public void continueOrPropagate(RetryableException exception) {
        if (attempt++ >= maxAttempts) {
            throw exception;
        }

        long delay = (long) (initialBackoffMillis * Math.pow(multiplier, attempt - 2));
        try {
            Thread.sleep(Math.max(0, delay));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw exception;
        }
    }

    @Override
    public Retryer clone() {
        return new ExponentialBackoffRetryer(initialBackoffMillis, multiplier, maxAttempts);
    }
}
