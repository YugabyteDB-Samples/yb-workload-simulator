package com.yugabyte.simulation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.retry")
public class RetryConfigProperties {
    private int maxAttempts = 3;
    private int backoffInitialInterval = 3500;
    private int backoffMultiplier = 3;
    private int backoffMaxInterval = 30000;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getBackoffInitialInterval() {
        return backoffInitialInterval;
    }

    public void setBackoffInitialInterval(int backoffInitialInterval) {
        this.backoffInitialInterval = backoffInitialInterval;
    }

    public int getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public void setBackoffMultiplier(int backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }

    public int getBackoffMaxInterval() {
        return backoffMaxInterval;
    }

    public void setBackoffMaxInterval(int backoffMaxInterval) {
        this.backoffMaxInterval = backoffMaxInterval;
    }
}
