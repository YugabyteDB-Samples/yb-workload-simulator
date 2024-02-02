package com.yugabyte.simulation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.util.regex.Pattern;

@Configuration
@EnableConfigurationProperties(RetryConfigProperties.class)
public class RetryConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryConfig.class);

    // 40001 - optimistic locking or leader changes abort
    // 40P01 - deadlock
    // 08006 - connection issues
    // 57P01 - broken pool conn (invalidated connections because of node failure, etc.)
    // XX000 - other connection related issues (not classified) <- removed as not explicitly retryable
    private static final Pattern SQL_STATE_PATTERN = Pattern.compile("^(40001)|(40P01)|(57P01)|(08006)|(XX000)|(42804)");

    /**
     * Configures a Spring Retry Backoff policy based on a randomized exponential backoff.
     * Exponential backoff uses a multiplier factor to determine the delay for the next retry.
     * <p>
     * This behaves nicely as it assumes the first retry is likely something minor and will
     * be resolved with the next connection and, if it fails again, that it may take longer with
     * each successive retry.
     * <p>
     * The addition of a randomized "jitter" helps reduce the impact of synchronized retry loops
     * all colliding with each other making the problem worse.
     * <p>
     * As a general rule of thumb, set the initial interval low so that a single retry does not
     * add too much latency of the original request (assuming a single retry will resolve 99.9% of
     * the time).  The multiplier should be fairly small as well but not so small that all the
     * retries are exhausted in < 3 seconds as this should cover the exceptional case of complete
     * network failure and tablet leader re-election in another zone/region.
     *
     * @return a configured BackOffPolicy
     */
    @Bean
    public BackOffPolicy exponentialRandomBackOffPolicy(RetryConfigProperties retryProperties) {
        ExponentialRandomBackOffPolicy randomBackOffPolicy = new ExponentialRandomBackOffPolicy();
        randomBackOffPolicy.setInitialInterval(retryProperties.getBackoffInitialInterval());
        randomBackOffPolicy.setMultiplier(retryProperties.getBackoffMultiplier());
        // max interval will set the upper bounds of any calculated interval so that no
        // single retry loop will ever wait longer than this value.
        randomBackOffPolicy.setMaxInterval(retryProperties.getBackoffMaxInterval());
        return randomBackOffPolicy;
    }

    /**
     * Configures a Spring Retry policy that handles nested exceptions specifically designed
     * to catch and retry specific SQL exceptions.  Since this cannot be determined entirely
     * by exception class, this retry policy also uses SQL State to determine if an execution
     * is retryable using a regular expression.  For any other class of execution, a no-op
     * retry policy will be used.
     *
     * @return a configured RetryPolicy
     */
    @Bean
    public RetryPolicy exceptionClassifierRetryPolicy(RetryConfigProperties retryProperties) {
        ExceptionClassifierRetryPolicy retryPolicy = new ExceptionClassifierRetryPolicy();

        // delegate retry policies based on the type of exception/sql state
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(retryProperties.getMaxAttempts());
        NeverRetryPolicy neverRetryPolicy = new NeverRetryPolicy();

        // Unroll the exception stack looking for:
        // SQLRecoverableException or SQLTransientConnectionException
        // OR any other SQLException that has a SqlState matching the
        // pattern of known retryable errors.  Otherwise, use a never-retry policy.

        retryPolicy.setExceptionClassifier(classifiable -> {
            while (classifiable != null) {
                if (classifiable instanceof SQLRecoverableException || classifiable instanceof SQLTransientConnectionException) {
                    return simpleRetryPolicy;
                } else if (classifiable instanceof SQLException ) {
                    SQLException ex = (SQLException) classifiable;
                    System.out.println("SQLState: " + ex.getSQLState() + " ErrorCode: " + ex.getErrorCode() + " Message: " + ex.getMessage());
                    // assumes SQLState is only populated with state codes
                    if (ex.getSQLState() != null && SQL_STATE_PATTERN.matcher(ex.getSQLState()).matches()) {
                        return simpleRetryPolicy;
                    }
                    else if(ex.getSQLState() == null){
                        return simpleRetryPolicy;
                    }
                }
                classifiable = classifiable.getCause();
            }

            return neverRetryPolicy; // never retry on anything else
        });

        return retryPolicy;
    }
}
